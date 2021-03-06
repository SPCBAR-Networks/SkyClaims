/*
 * SkyClaims - A Skyblock plugin made for Sponge
 * Copyright (C) 2017 Mohron
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SkyClaims is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SkyClaims.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.mohron.skyclaims.command.team;

import net.mohron.skyclaims.command.CommandBase;
import net.mohron.skyclaims.command.CommandIsland;
import net.mohron.skyclaims.permissions.Options;
import net.mohron.skyclaims.permissions.Permissions;
import net.mohron.skyclaims.team.Invite;
import net.mohron.skyclaims.team.PrivilegeType;
import net.mohron.skyclaims.world.Island;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

public class CommandInvite extends CommandBase.IslandCommand {

  public static final String HELP_TEXT = "used to invite players to your island or list your pending invites.";
  private static final Text LIST = Text.of("list");
  private static final Text USER = Text.of("user");
  private static final Text PRIVILEGE = Text.of("privilege");

  public static void register() {
    CommandSpec commandSpec = CommandSpec.builder()
        .permission(Permissions.COMMAND_INVITE)
        .arguments(GenericArguments.optional(GenericArguments.firstParsing(
            GenericArguments.seq(
                GenericArguments.user(USER),
                GenericArguments.optional(PrivilegeType.getCommandArgument(PRIVILEGE), PrivilegeType.MEMBER)
            ),
            GenericArguments.literal(LIST, "list")
        )))
        .description(Text.of(HELP_TEXT))
        .executor(new CommandInvite())
        .build();

    try {
      CommandIsland.addSubCommand(commandSpec, "invite");
      PLUGIN.getGame().getCommandManager().register(PLUGIN, commandSpec);
      PLUGIN.getLogger().debug("Registered command: CommandInvite");
    } catch (UnsupportedOperationException e) {
      PLUGIN.getLogger().error("Failed to register command: CommandInvite", e);
    }
  }

  @Override
  public CommandResult execute(Player player, Island island, CommandContext args) throws CommandException {
    User user = args.<User>getOne(USER).orElse(null);
    PrivilegeType type = args.<PrivilegeType>getOne(PRIVILEGE).orElse(PrivilegeType.MEMBER);
    int maxTeammates = Options.getMaxTeammates(island.getOwnerUniqueId());

    if (type == PrivilegeType.NONE) {
      throw new CommandException(Text.of(TextStyles.ITALIC, "What kind of invite is ", TextStyles.RESET, type.toText(), TextStyles.ITALIC, "?"));
    }

    if (user == null || args.hasAny(LIST)) {
      PLUGIN.getInviteService().listIncomingInvites().accept(player);
    } else if (player.equals(user)) {
      throw new CommandException(Text.of(TextColors.RED, "You cannot invite yourself!"));
    } else if (island.getPrivilegeType(user) != PrivilegeType.NONE) {
      throw new CommandException(Text.of(
          island.getPrivilegeType(user).format(user.getName()), TextColors.RED, " is already a ",
          island.getPrivilegeType(user).toText(),
          TextColors.RED, " on ", island.getName(), TextColors.RED, "!"
      ));
    } else if (!island.isOwner(player) || island.getPrivilegeType(player).ordinal() >= type.ordinal()) {
      throw new CommandException(Text.of(
          TextColors.RED, "You do not have permission to send ",
          type == PrivilegeType.OWNER ? "an " : "a ",
          type.toText(), TextColors.RED, " invite for ", island.getName(), TextColors.RED, "!"
      ));
    } else if (maxTeammates > 0 && island.getTotalMembers() >= maxTeammates) {
      throw new CommandException(Text.of(island.getName(), TextColors.RED, " has reached its maximum team size (", maxTeammates, ")!"));
    } else {
      Invite.builder()
          .island(island)
          .sender(player)
          .receiver(user)
          .privilegeType(type)
          .build()
          .send();
      player.sendMessage(Text.of(TextColors.GREEN, "Island invite sent to ", type.format(user.getName()), TextColors.GREEN, "."));
    }

    return CommandResult.success();
  }

}
