package thediamondcg.ipsecuritysuite

import com.mojang.authlib.GameProfile
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import kotlinx.serialization.json.*
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.command.argument.GameProfileArgumentType
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.CommandManager.*
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.text.Text
import okhttp3.Request
import okhttp3.Response

val MOD_ID: String = "ip-security-suite"

class IPSecuritySuite : ModInitializer {
    var ipData: IPDataManager = IPDataManager()

    override fun onInitialize() {
        var root = literal("ipss");

//        var locking = root.then(literal("lock").requires { ipData.ipData.canLockForSelf ?: true }.executes(this::selfLock))
//        var lock_other = locking.then(argument("player_name", GameProfileArgumentType.gameProfile()).executes(this::ipLock)).requires(this::hasOp)
//        var lock_last = locking.then(literal("last")).requires(this::hasOp)
//            .then(argument("player_name", GameProfileArgumentType.gameProfile())).executes(this::authLastIp)
//        var strict_mode = root.then(literal("strict")).requires(this::hasOp)
//        var set_strict_mode = strict_mode.then(literal("set").requires(this::hasOp).then(
//            argument("value", BoolArgumentType.bool()).executes(this::strictMode)
//        ))
////        var get_strict_mode = strict_mode.then(literal("get").requires(this::hasOp).executes(this::getStrictMode))
        var locking = literal("lock")
        locking = locking.requires(this::hasSelfLock).executes(this::selfLock)
        locking = locking.then(literal("last").then(argument("player_name", GameProfileArgumentType.gameProfile()).requires(this::hasOp).executes(this::authLastIp)))
        locking = locking.then(literal("other").then(argument("player_name", GameProfileArgumentType.gameProfile()).requires(this::hasOp).executes(this::ipLock)))
        locking = locking.then(literal("erase").then(argument("player_name", GameProfileArgumentType.gameProfile()).requires(this::hasOp).executes(this::eraseIps)))

        var vpn = literal("vpn").requires(this::hasOp)
        vpn = vpn.then(literal("api").then(
            argument("api_key", StringArgumentType.string()).executes(this::setApiKey)
        ))
        var config = literal("config").requires(this::hasOp)
        var config_set = literal("set")
        var config_get = literal("get")

        config_get = config_get.then(literal("strict").executes(this::getStrictMode))
        config_get = config_get.then(literal("vpn").executes(this::getVpnMode))
        config_get = config_get.then(literal("self-lock").executes(this::getSelfLockMode))

        config_set = config_set.then(literal("strict").then(
            argument("value", BoolArgumentType.bool()).executes(this::strictMode)
        ))
        config_set = config_set.then(literal("vpn").then(
            argument("value", BoolArgumentType.bool()).executes(this::setVpn)
        ))
        config_set = config_set.then(literal("self-lock").then(
            argument("value", BoolArgumentType.bool()).executes(this::selfLockMode)
        ))

        config = config.then(config_set)
        config = config.then(config_get)

        root = root.then(config)
        root = root.then(vpn)
        root = root.then(locking)

//        val ipLock = literal("ip-lock").requires { src: ServerCommandSource -> src.hasPermissionLevel(2) }
//            .then(argument("player_name", GameProfileArgumentType.gameProfile()).executes(this::ipLock))
//        val lockLast = literal("ip-lock-last").requires { src: ServerCommandSource -> src.hasPermissionLevel(2) }
//            .then(argument("player_name", GameProfileArgumentType.gameProfile()).executes(this::authLastIp))
//        val setStrictMode =
//            literal("ip-strict-mode").requires { src: ServerCommandSource -> src.hasPermissionLevel(2) }.then(
//                argument("value", BoolArgumentType.bool()).executes(this::strictMode)
//            )
//        val setVpn =
//            literal("ip-allow-vpn").requires { src: ServerCommandSource -> src.hasPermissionLevel(2) }.then(
//                argument("value", BoolArgumentType.bool()).executes(this::setVpn)
//            )
//
//        val selfLockMode =
//            literal("ip-allow-self-locking").requires { src: ServerCommandSource -> src.hasPermissionLevel(2) }.then(
//                argument("value", BoolArgumentType.bool()).executes(this::selfLockMode)
//            )
//        val selfLock =
//            literal("ip-lock-self").requires { _: ServerCommandSource -> ipData.ipData.canLockForSelf ?: true }
//                .executes(this::selfLock)
//        val eraseIp = literal("ip-erase-all").requires { src: ServerCommandSource -> src.hasPermissionLevel(2) }.then(
//            argument("player_name", GameProfileArgumentType.gameProfile()).executes(this::eraseIps)
//        )
//        val setApiKey =
//            literal("ip-set-api-key").requires { src: ServerCommandSource -> src.hasPermissionLevel(2) }.then(
//                argument("api_key", StringArgumentType.string()).executes(this::setApiKey)
//            );

        CommandRegistrationCallback.EVENT.register { commandDispatcher: CommandDispatcher<ServerCommandSource>,
                                                     _commandRegistryAccess: CommandRegistryAccess,
                                                     _registrationEnvironment: RegistrationEnvironment ->
//            commandDispatcher.register(ipLock)
//            commandDispatcher.register(lockLast)
//            commandDispatcher.register(selfLockMode)
//            commandDispatcher.register(selfLock)
//            commandDispatcher.register(setStrictMode)
//            commandDispatcher.register(setVpn)
//            commandDispatcher.register(eraseIp)
//            commandDispatcher.register(setApiKey)
            commandDispatcher.register(root)
        }

        ServerPlayConnectionEvents.INIT.register { serverPlayNetworkHandler: ServerPlayNetworkHandler,
                                                   minecraftServer: MinecraftServer ->
            try {
                val player = serverPlayNetworkHandler.player

                ipData.ipData.lastIp[player.uuid.toString()] = player.ip

                val validIp = ipData.validate(player.uuid, player.ip)

                val vpnApiKey = ipData.getApiKey();

                println(!validIp)
                println(ipData.ipData.vpnAllowed == false)
                println(vpnApiKey != null)

                if (!validIp) {
                    serverPlayNetworkHandler.disconnect(Text.literal("Someone tried logging in with unauthorized IP: " + player.ip))
                    minecraftServer.sendMessage(Text.literal("Someone unauthorized tried to login to " + player.name + "'s account!"))
                }

                if ((ipData.ipData.vpnAllowed == false) && !ipData.strictlyValidIp(player.uuid, player.ip) && (vpnApiKey != null)) {
                    val client = okhttp3.OkHttpClient();

                    val response: Response = client.newCall(
                        Request.Builder().get().url("https://vpnapi.io/api/${player.ip}?key=${vpnApiKey}").build()
                    ).execute()

                    println("https://vpnapi.io/api/${player.ip}?key=${vpnApiKey}")
                    println(response.body)

                    if(response.body != null) {
                        val body: String = response.body!!.string();

                        println(body)

                        val data = Json.parseToJsonElement(body)

                        println(data)

                        val isVpn: Boolean = data.jsonObject["security"]?.jsonObject?.containsValue(Json.parseToJsonElement("true")) == true

                        if(isVpn) {
                            serverPlayNetworkHandler.disconnect(Text.literal("VPNs are not allowed."))
                        }
                    }
                }

                ipData.saveData()
            } catch(e: RuntimeException) {
                println("For some reason, authentication failed!?")
                e.printStackTrace()
            }

        }
    }

    private fun ipLock(ctx: CommandContext<ServerCommandSource>): Int {
        // When an operator does the command with @a, it can select several profiles.
        val selectedProfiles = GameProfileArgumentType.getProfileArgument(ctx, "player_name")

        for (playerProfile: GameProfile in selectedProfiles) {
            val playerUuid = playerProfile.id

            val playerIp = ctx.source.server.playerManager.getPlayer(playerUuid)?.ip

            if (playerIp != null) {
                ipData.lockIp(playerUuid, playerIp)

                ipData.saveData()

                ctx.source.sendFeedback({
                    Text.literal("Player with username '" + playerProfile.name.toString() + "' and UUID '" + playerUuid + "' has had their IP locked to " + playerIp)
                }, true)
            } else {
                ctx.source.sendFeedback({
                    Text.literal("Player with username '" + playerProfile.name.toString() + "' and UUID '" + playerUuid + "' did not get their IP locked due to an error. Whoops...")
                }, true)
            }
        }

        return 1
    }

    private fun eraseIps(ctx: CommandContext<ServerCommandSource>): Int {
        // When an operator does the command with @a, it can select several profiles.
        val selectedProfiles = GameProfileArgumentType.getProfileArgument(ctx, "player_name")

        for (playerProfile: GameProfile in selectedProfiles) {
            val playerUuid = playerProfile.id

            if (ipData.eraseIPs(playerUuid)) {
                ctx.source.sendFeedback({
                    Text.literal("Player with username '" + playerProfile.name + "' and UUID '" + playerUuid + "' has had their past IPs erased.")
                }, true)

                ipData.saveData()
            }
        }

        return 1
    }

    private fun selfLock(ctx: CommandContext<ServerCommandSource>): Int {
        val playerProfile = ctx.source.player ?: return 2

        val playerUuid = playerProfile.uuid ?: return 2

        val playerIp = playerProfile.ip ?: return 2


        ipData.lockIp(playerUuid, playerIp)

        ipData.saveData()

        ctx.source.sendFeedback({
            Text.literal("Locked account with username '" + playerProfile.name.string + "' and UUID '" + playerUuid + "' to the IP " + playerIp)
        }, true)


        return 1
    }

    private fun authLastIp(ctx: CommandContext<ServerCommandSource>): Int {
        val playerProfile = GameProfileArgumentType.getProfileArgument(ctx, "player_name").first()

        val playerUuid = playerProfile.id

        val lastPlayerIp = ipData.getLast(playerUuid)

        if (lastPlayerIp != null) {
            ipData.lockIp(playerUuid, lastPlayerIp)

            ctx.source.sendFeedback({
                Text.literal("Player with username '" + playerProfile.name + "' and UUIDa '" + playerUuid + "' has had their IP locked to their last logon IP, which was " + lastPlayerIp)
            }, true)

            ipData.saveData()
        } else {
            ctx.source.sendFeedback({
                Text.literal("Player with username '" + playerProfile.name + "' and UUID '" + playerUuid + "' did not get their last IP locked because there is no record of them ever logging on.")
            }, true)
        }



        return 1
    }

    private fun strictMode(ctx: CommandContext<ServerCommandSource>): Int {
        val strictModeSetting = BoolArgumentType.getBool(ctx, "value")

        if (strictModeSetting) {
            ipData.enableStrict()
            ctx.source.sendFeedback({
                Text.literal("Strict mode is enabled. This means any accounts without a pre-existing IP lock cannot join!")
            }, true)
        } else {
            ipData.disableStrict()
            ctx.source.sendFeedback({
                Text.literal("Strict mode is disabled. This means non-IP-locked accounts can join from any IP!")
            }, true)
        }

        ipData.saveData()

        return 1
    }

    private fun getStrictMode(ctx: CommandContext<ServerCommandSource>): Int {
        if(ipData.ipData.strictMode == true) {
            ctx.source.sendFeedback({
                Text.literal("Strict mode is enabled. This means any accounts without a pre-existing IP lock cannot join!")
            }, false)
        } else {
            ipData.disableStrict()
            ctx.source.sendFeedback({
                Text.literal("Strict mode is disabled. This means non-IP-locked accounts can join from any IP!")
            }, false)
        }

        return 1
    }

    private fun selfLockMode(ctx: CommandContext<ServerCommandSource>): Int {
        val selfLockSetting = BoolArgumentType.getBool(ctx, "value")

        ipData.setSelfLocking(selfLockSetting)

        if (selfLockSetting) {
            ctx.source.sendFeedback({
                Text.literal("Self-locking is enabled. This means players can register their own IPs when they first log on. Works best when strict mode is set to false.")
            }, true)
        } else {
            ctx.source.sendFeedback({
                Text.literal("Self-locking is disabled. This means operators have to register players' IPs when they log on.")
            }, true)
        }

        ipData.saveData()

        return 1
    }

    private fun getSelfLockMode(ctx: CommandContext<ServerCommandSource>): Int {
        if(ipData.ipData.canLockForSelf != false) {
            ctx.source.sendFeedback({
                Text.literal("Self-locking is enabled. This means players can register their own IPs when they first log on. Works best when strict mode is set to false.")
            }, false)
        } else {
            ctx.source.sendFeedback({
                Text.literal("Self-locking is disabled. This means operators have to register players' IPs when they log on.")
            }, false)
        }

        return 1;
    }

    private fun setVpn(ctx: CommandContext<ServerCommandSource>): Int {
        val vpnSetting = BoolArgumentType.getBool(ctx, "value")

        ipData.setVpnAllowed(vpnSetting)

        if (vpnSetting) {
            ctx.source.sendFeedback({
                Text.literal("VPN logins are now enabled.")
            }, true)
        } else {
            if(ipData.getApiKey() == null) {
                ctx.source.sendFeedback({
                    Text.literal("You don't seem to have a vpnapi.io API key setup. This means that this option will not work. Set an api key with /ipss vpn api <YOURKEYHERE>")
                }, false)
            }
            ctx.source.sendFeedback({
                Text.literal("VPN logins are no longer enabled. Note that if you run out of API requests, all IPs are allowed to connect.")
            }, true)
        }

        ipData.saveData()

        return 1
    }

    private fun getVpnMode(ctx: CommandContext<ServerCommandSource>): Int {
        if(ipData.ipData.vpnAllowed == false) {
            ctx.source.sendFeedback({
                Text.literal("VPN logins are not enabled. Note that if you run out of API requests, all IPs are allowed to connect.")
            }, false)
        } else {
            ctx.source.sendFeedback({
                Text.literal("VPN logins are enabled.")
            }, false)
        }

        return 1;
    }

    private fun setApiKey(ctx: CommandContext<ServerCommandSource>): Int {
        val perpetrator = ctx.source.player?.name?.string ?: "the server terminal";
        val apiKey = StringArgumentType.getString(ctx, "api_key");

        ipData.setApiKey(apiKey)

        ctx.source.sendFeedback({
            Text.literal("The VPN API key has been updated by $perpetrator.")
        }, true)

        ipData.saveData()

        return 1
    }

    private fun hasOp(src: ServerCommandSource): Boolean {
        return src.hasPermissionLevel(3)
    }

    private fun hasSelfLock(src: ServerCommandSource): Boolean {
        return ((ipData.ipData.canLockForSelf ?: true) || this.hasOp(src))
    }
}