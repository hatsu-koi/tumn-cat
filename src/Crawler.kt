package com.tumn.cat

import org.jsoup.Jsoup
import java.util.Date

abstract class Crawler {
	abstract fun crawl()
}

data class Content(val type: String, val url: String, val content: String, val date: Date)

val NAVER_NEWS_URI = ""

class NaverNewsCrawler: Crawler() {

	override fun crawl() {
	}

	override fun toString(): String = "Naver News Crawler"
}
