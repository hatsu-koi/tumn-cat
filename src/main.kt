package com.tumn.cat

import com.beust.klaxon.Klaxon
import com.mongodb.client.MongoClients
import com.tumn.cat.crawler.DcCrawler
import com.tumn.cat.crawler.IlbeCrawler
import com.tumn.cat.crawler.NaverNewsCrawler
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.logging.Level
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

	Logger.getLogger("org.mongodb.driver.connection").level = Level.OFF
	Logger.getLogger("org.mongodb.driver.management").level = Level.OFF
	Logger.getLogger("org.mongodb.driver.cluster").level = Level.OFF
	Logger.getLogger("org.mongodb.driver.protocol.insert").level = Level.OFF
	Logger.getLogger("org.mongodb.driver.protocol.query").level = Level.OFF
	Logger.getLogger("org.mongodb.driver.protocol.update").level = Level.OFF

	val c = MongoClients.create(config.host).getDatabase(config.db).getCollection("contents")

	print("""Crawled data will be saved to: %s

		| Available crawling sites
		|* 1> Naver News
		|* 2> DcInside
		|* 3> Ilbe
		|* Select website to crawl from: """.trimMargin().format(config.host))

	when(readLine()!!){
		"1" -> {
			print("""The crawler will crawl comments from headlines between two dates.
				|* Please provide the date to start with (yyyy-mm-dd): """.trimMargin())

			val formatter = SimpleDateFormat("yyyy-MM-dd")
			val start = formatter.parse(readLine()!!)

			print("* Please provide the date to end with (yyyy-mm-dd): ")
			val end = formatter.parse(readLine()!!)

			NaverNewsCrawler(c, config.userAgent, 1, start, end).start()
		}
		"2" -> {
			print("""The crawler will crawl threads from DCInside.
				|* Please provide number of threads to look up: """.trimMargin())
			val num = readLine()!!.toInt()
			print("""* Please provide board id: """)
			val boardId = readLine()!!

			DcCrawler(c, config.userAgent, 200, boardId, num).start()
		}
		"3" -> {
			print("""The crawler will crawl threads from Ilbe.
				|* Please provide number of threads to look up: """.trimMargin())
			val num = readLine()!!.toInt()
			print("""* Please provide board id: """)
			val boardId = readLine()!!

			IlbeCrawler(c, config.userAgent, 200, boardId, num).start()
		}
		else -> println("Unavailable option selected. Exiting.")
	}
}

data class Config(val host: String, val db: String, val userAgent: String)