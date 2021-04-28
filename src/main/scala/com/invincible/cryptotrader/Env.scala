package com.invincible.cryptotrader

import com.invincible.cryptotrader.Models.Environment

import java.util.Properties
import scala.io.Source

object Env {

  def readEnvironment(filename:String) = {
    val url = getClass.getResource(filename)
    if (url != null) {
      val source = Source.fromURL(url)
      val p = new Properties()
      p.load(source.bufferedReader())
      Environment(p.getProperty("url"),p.getProperty("apiKey"),p.getProperty("apiSecret"))
    } else null
  }
}