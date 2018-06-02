package com.tumn.cat.crawler

import com.mongodb.client.MongoCollection
import com.tumn.cat.Crawler
import com.tumn.cat.isInteger
import org.bson.Document

const val DC_LIST_URI = "http://gall.dcinside.com/board/lists/?id=%s&page=%d"
const val DC_THREAD_URI = "http://gall.dcinside.com/board/view/?id=%s&no=%d"

class DcCralwer(
		collection: MongoCollection<Document>,
		userAgent: String,
		interval: Long,
		private val boardId: String,
		private val count: Int
): Crawler(collection, userAgent, interval) {
	override fun crawl(){
		// TODO
		var page = 1
		var num = count

		connectAndGet(DC_LIST_URI.format(boardId, page)).thenApply {
			if(num > 0) {
				val id = it.select("t_notice").text()
				if (id.isInteger()) { // '공지' ID를 걸러냄
					connectAndGet(DC_THREAD_URI.format(boardId, id)).thenApply {
						it.select("div.s_write>table>tbody").text()
					}

					num--
				}
			}
		}.join()

		close()
	}
}
