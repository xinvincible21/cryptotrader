package com.invincible.cryptotrader

import com.invincible.cryptotrader.CryptoCancelAll.cancelAllPath
import com.invincible.cryptotrader.Models.Environment
import org.apache.commons.codec.digest.{HmacAlgorithms, HmacUtils}

import java.io.{BufferedReader, InputStreamReader}
import java.math.BigDecimal
import java.net.{HttpURLConnection, URL}
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import java.util.{Base64, Scanner, UUID}


object CryptoCancelOrder {

  val sandbox = Env.readEnvironment("/sandbox.properties")
  val prod = Env.readEnvironment("/prod.properties")
  val cancelOrderPath = "/v1/order/cancel"
  val makerFeeRate   = new BigDecimal(0.001)
  val takerFeeRate   = new BigDecimal(0.0035)

  val decimalFormat = new DecimalFormat("###.##")

  object Side extends Enumeration {
    type Side = Value
    val Buy, Sell = Value
  }

  def main(args: Array[String]): Unit = {

    val scanner = new Scanner(System.in)
    println("Prod?")
    val isProd = scanner.next().toLowerCase == "prod"
    println("Order ID?")
    val orderID = scanner.next().toLong
    println(
      s"""
         |order to be cancelled
         |$orderID
         |""".stripMargin
    )
    val environment = if (isProd) {
      println("THIS IS PROD!!!!!!!!!!!!!!!!!!!!!!!!!")
      prod
    } else sandbox
    println(environment.url)
    println("Confirm Y/N?")
    val confirmation = scanner.next()
    confirmation.toLowerCase match {
      case "y" =>
        placeOrder(orderID, environment)
      case "_" => println("Exiting")

    }
  }
  def placeOrder(orderID:Long, environment: Environment) = {

    val key     = environment.apiSecret.getBytes(StandardCharsets.UTF_8)
    val hm384   = new HmacUtils(HmacAlgorithms.HMAC_SHA_384, key)
    val orderId = UUID.randomUUID()

    val nonce = System.currentTimeMillis()

    val payload =
      s"""
         |{
         |  "request": "$cancelOrderPath",
         |  "nonce": "$nonce",
         |  "client_order_id": "$orderId"
         |}
         |""".stripMargin
    val encoder   = Base64.getEncoder
    val b64       = encoder.encode(payload.getBytes(StandardCharsets.UTF_8))
    val signature = hm384.hmacHex(b64)
    println(payload)
    println(new String(b64, StandardCharsets.UTF_8))
    println(signature)

    val requestHeaders = Map[String, String](
      "Content-Type"       -> "text/plain",
      "Content-Length"            -> "0",
      "X-GEMINI-APIKEY"           -> environment.apiKey,
      "X-GEMINI-PAYLOAD"          -> new String(b64, StandardCharsets.UTF_8),
      "X-GEMINI-SIGNATURE"        -> signature,
      "Cache-Control"             -> "no-cache"
    )

    val urlPath = new URL(environment.url + cancelOrderPath)
    val con     = urlPath.openConnection.asInstanceOf[HttpURLConnection]
    requestHeaders.foreach(x => con.setRequestProperty(x._1, x._2))
    con.setRequestMethod("POST")
    con.setDoOutput(false)
    con.setDoInput(true)

    try {
      val br = new BufferedReader(new InputStreamReader(con.getInputStream))
      try {
        var line = br.readLine()
        while (line != null) {
          println(line)
          line = br.readLine()
        }
      } finally br.close
    } catch {
      case e:Throwable => println(e.getMessage)
    }
  }

}
