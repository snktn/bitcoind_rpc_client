import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.*
import kotlin.jvm.Throws

class BitcoindRPCConnection (private val url: URL) {
    @Throws(ConnectException::class, IOException::class)
    fun getRawTxAddresses(id: String, tx: String) : ArrayList<String> {
        val list = ArrayList<String>()
        val jsonInputString = rpcString(id, "getrawtransaction", "\"$tx\", true" )
        "\"addresses\":\\[\"(\\w+)".toRegex().findAll(getResponse(jsonInputString)).forEach {
            list.add(it.groupValues[1])
        }
        return list
    }

    @Throws(ConnectException::class, IOException::class)
    fun getRawMemPool(id: String) : ArrayList<String> {
        val list = ArrayList<String>()
        val jsonInputString = rpcString(id, "getrawmempool" )
        "\\w{64}".toRegex().findAll(getResponse(jsonInputString)).forEach {
            list.add(it.value)
        }
        return list
    }

    @Throws(ConnectException::class, IOException::class)
    fun getBlockCount(id: String) : Int? {
        val jsonInputString = rpcString(id, "getblockcount")
        return ("\\d+".toRegex().find(getResponse(jsonInputString))?.value?.toInt())
    }

    fun auth (userName: String, passWord: String) {
        Authenticator.setDefault(object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(userName, passWord.toCharArray())
            }
        })
    }

    private fun rpcString (id: String, method: String, params: String = "") : String {
        return "{\"jsonrpc\":\"1.0\",\"id\":\"$id\",\"method\":\"$method\",\"params\":[$params]}"
    }

    @Throws(ConnectException::class)
    fun connect (jsonInputString: String) : HttpURLConnection {
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json; utf-8")
        connection.setRequestProperty("Accept", "application/json")
        connection.doOutput = true
        connection.outputStream.use { outputStream ->
            val input = jsonInputString.toByteArray(charset("utf-8"))
            outputStream.write(input, 0, input.size)
        }
        return connection
    }

    @Throws(IOException::class)
    private fun getResponse (jsonInputString: String ) : String {
        val response = StringBuilder()
        val connection = connect(jsonInputString)
        BufferedReader(InputStreamReader(connection.inputStream, "utf-8")).use { bufferedReader ->
            var responseLine: String?
            while (bufferedReader.readLine().also { responseLine = it } != null) {
                response.append(responseLine!!.trim { it <= ' ' })
            }
        }
        return response.toString()
    }
}