package com.tumn.cat

fun String.isInteger(): Boolean {
	return this.matches(Regex("^\\d+$"))
}
