package com.tumn.cat

import com.beust.klaxon.Klaxon
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat

fun main(args: Array<String>){
	if(!File("config.json").exists()) {
		Object::class.java.getResourceAsStream("/config.json").use {input ->
			FileOutputStream(File("config.json")).use {
				input.copyTo(it)
			}
		}
	}

	val config = Klaxon().parse<Config>(File("config.json"))!!

	print("""Crawled data will be saved to: %s

		| Available crawling sites
		|* 1> Naver News
		|* Select website to crawl from: """.trimMargin().format(config.host))

	when(readLine()!!){
		"1" -> {
			print("""The crawler will crawl comments from headlines between two dates.
				|* Please provide the date to start with (yyyy-mm-dd):
			""".trimMargin())

			val formatter = SimpleDateFormat("yyyy-MM-dd")
			val start = formatter.parse(readLine()!!)

			print("* Please provide the date to end with (yyyy-mm-dd): ")
			val end = formatter.parse(readLine()!!)

			NaverNewsCrawler(config.userAgent, 200, start, end).crawl()
		}
		else -> println("Unavailable option selected. Exiting.")
	}
}

data class Config(val host: String, val userAgent: String)