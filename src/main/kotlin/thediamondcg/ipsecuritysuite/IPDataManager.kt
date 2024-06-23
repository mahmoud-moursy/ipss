package thediamondcg.ipsecuritysuite

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import net.fabricmc.loader.api.FabricLoader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.collections.HashMap

@Serializable
data class IPData(
    // vpnapi.io API key
    var apiKey: String?,
    // A list of all IP locks
    var ipLocks: HashMap<String, HashSet<String>>,
    // Last IP used to log on
    var lastIp: HashMap<String, String>,
    // Strict mode makes it so that all players must be registered to log on.
    var strictMode: Boolean?,
    // Toggle whether members can lock their own IP.
    var canLockForSelf: Boolean?,
    // Toggles whether to allow VPNs or proxies
    var vpnAllowed: Boolean?
)


@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
class IPDataManager {
    private val configData: File = FabricLoader.getInstance().configDir.resolve("ip-security-suite.config.json").toFile()
    var ipData: IPData

    init {
        configData.createNewFile() // Create the config file ONLY if it does not already exist
        val inputStream: InputStream = configData.inputStream()
        try {
            ipData = Json.decodeFromStream<IPData>(inputStream)
        } catch (e: RuntimeException) {
            ipData = IPData(apiKey = null, canLockForSelf = null, ipLocks = HashMap(), lastIp = HashMap(), strictMode = null, vpnAllowed = null)
            this.saveData()
        }
    }


    fun setApiKey(apiKey: String) {
        ipData.apiKey = apiKey

        val outputStream: OutputStream = configData.outputStream()
    }

    fun getApiKey(): String? = ipData.apiKey

    fun setVpnAllowed(enable: Boolean) {
        ipData.vpnAllowed = enable
    }

    fun getIps(uuid: UUID): HashSet<String>? {
        val uuidAsString: String = uuid.toString()

        return ipData.ipLocks[uuidAsString]
    }

    fun eraseIPs(uuid: UUID): Boolean {
        val uuidAsString: String = uuid.toString();

        val wasEmpty = ipData.ipLocks.isNotEmpty();

        ipData.ipLocks[uuidAsString]?.clear()
        ipData.lastIp.remove(uuidAsString);

        return wasEmpty;
    }

    fun getLast(uuid: UUID): String? {
        val uuidAsString: String = uuid.toString()

        return ipData.lastIp[uuidAsString]
    }

    fun lockIp(uuid: UUID, ip: String) {
        val uuidAsString: String = uuid.toString()

        this.ipData.ipLocks.getOrPut(uuidAsString, { HashSet() }).add(ip);
    }

    fun validate(uuid: UUID, ip: String): Boolean {
        val ips = this.getIps(uuid)

        // Login is valid if no valid IPs registered, or if strict mode is disabled. By default, strict mode is disabled.
        if(ips.isNullOrEmpty() && ipData.strictMode != true) {
            return true
        }

        return ips?.contains(ip) == true
    }

    fun strictlyValidIp(uuid: UUID, ip: String): Boolean {
        val ips = this.getIps(uuid)

        return ips?.contains(ip) == true
    }

    fun enableStrict() {
        ipData.strictMode = true
    }

    fun disableStrict() {
        ipData.strictMode = false
    }

    fun saveData() {
        Json.encodeToStream(ipData, configData.outputStream())
    }

    fun setSelfLocking(value: Boolean) {
        ipData.canLockForSelf = value
    }
}