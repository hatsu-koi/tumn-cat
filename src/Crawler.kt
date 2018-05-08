package com.tumn.cat

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

abstract class Crawler(
		protected val userAgent: String = "Tumn-Cat_Bot/v1.0",
		protected val interval: Long
	) {
	abstract fun crawl()

	fun connectAndGet(uri: String): Document = Jsoup.connect(uri).userAgent(userAgent).get()
}

data class Content(val uri: String, val content: String)

val NAVER_NEWS_HEADLINES_URI: String = "http://news.naver.com/main/history/mainnews/index.nhn?date=%s&time=00:00"

class NaverNewsCrawler(
		userAgent: String,
		interval: Long,
		private val from: Date,
		private val to: Date
	):
		Crawler(userAgent, interval) {
	init {
		if(to.before(from)){
			throw IllegalArgumentException("Invalid range of date given")
		}
	}

	override fun crawl() {
		val c = Calendar.getInstance()
		c.time = from
		while(c.time.before(to)){
			println("Crawl %s".format(c.time.toString()))

			getListForDate(c.time).forEach {
				println("Found %s".format(it))

				val uri = URI(it)
				if(uri.host == "history.news.naver.com") {
					getHistoryNewsList(it).forEach { history ->
						println("History> %s".format(history))

						getCommentsList(history).forEach(System.out::println) // TODO save to database
					}
				}else if(uri.host == "news.naver.com"){
					println("Comment> %s".format(it))

					getCommentsList(it).forEach(System.out::println) // TODO
				}
			}

			Thread.sleep(interval)

			c.add(Calendar.DATE, 1)
		}
	}

	private fun getListForDate(date: Date): ArrayList<String> {
		val list = arrayListOf<String>()

		val formatter = SimpleDateFormat("yyyy-MM-dd")

		connectAndGet(NAVER_NEWS_HEADLINES_URI.format(formatter.format(date))).run {
			select("div.newsnow_tx_inner a").forEach {
				it.attr("href")?.let { href -> list.add(href) }
			}
		}

		return list
	}

	private fun getHistoryNewsList(uri: String): ArrayList<String> {
		val list = arrayListOf<String>()

		connectAndGet(uri).select("div.hissue_cnt a").forEach {
			it.attr("href")?.let { href ->
				if(URI(href).host == "news.naver.com") {
					list.add(href)
				}
			}
		}

		return list
	}

	private fun getCommentsList(uri: String): ArrayList<Content> {
		val list = arrayListOf<Content>()

		connectAndGet(uri).select("span.u_cbox_contents").forEach {
			list.add(Content(uri, it.text()))
		}

		return list
	}

	override fun toString(): String = "Naver News Crawler"
}
