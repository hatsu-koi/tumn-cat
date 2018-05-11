package com.tumn.cat

import com.beust.klaxon.Klaxon
import com.mongodb.client.MongoClients
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger


fun main(args: Array<String>){
	if(!File("config.json").exists()) {
		Object::class.java.getResourceAsStream("/config.json").use {input ->
			FileOutputStream(File("config.json")).use {
				input.copyTo(it)
			}
		}
	}

	val config = Klaxon().parse<Config>(File("config.json"))!!

	Logger.getLogger("org.mongodb.driver.connection").setLevel(Level.OFF);
	Logger.getLogger("org.mongodb.driver.management").setLevel(Level.OFF);
	Logger.getLogger("org.mongodb.driver.cluster").setLevel(Level.OFF);
	Logger.getLogger("org.mongodb.driver.protocol.insert").setLevel(Level.OFF);
	Logger.getLogger("org.mongodb.driver.protocol.query").setLevel(Level.OFF);
	Logger.getLogger("org.mongodb.driver.protocol.update").setLevel(Level.OFF)

	val c = MongoClients.create(config.host).getDatabase(config.db).getCollection("contents")

	print("""Crawled data will be saved to: %s

		| Available crawling sites
		|* 1> Naver News
		|* Select website to crawl from: """.trimMargin().format(config.host))

	when(readLine()!!){
		"1" -> {
			print("""The crawler will crawl comments from headlines between two dates.
				|* Please provide the date to start with (yyyy-mm-dd): """.trimMargin())

			val formatter = SimpleDateFormat("yyyy-MM-dd")
			val start = formatter.parse(readLine()!!)

			print("* Please provide the date to end with (yyyy-mm-dd): ")
			val end = formatter.parse(readLine()!!)

			NaverNewsCrawler(c, config.userAgent, 200, start, end).crawl()
		}
		else -> println("Unavailable option selected. Exiting.")
	}
}

data class Config(val host: String, val db: String, val userAgent: String)