package org.im.polyloader.utils

import org.im.polyloader.Polyloader

// Sufficient only for most mod.hjson files, but not all
// Moreover we assume this config is perfectly fine
class SimpleHJsonParser(private val hjson: String) {

    private var pos : Int = 0
        set(value) { field = if (value > hjson.length) hjson.length else value }

    // is it an End Of File?
    private val eof: Boolean
        get() = pos >= hjson.length

    private val data : MutableMap<String, Any?> = HashMap()

    fun get(key: String) = data[key]

    init {
        parseHjson()
    }
    private fun parseHjson() {
        while (!eof) {
            skip()
            parseMember()
        }
    }

    private fun parseMember() {
        val key : String = if (next() == '\'' || next() == '\"')
            parseJsonString() else parseQuotelessString(true)
        //Polyloader.log.debug("Got key: \'$key\'")
        skip()
        if (next() != ':')
            error("Found non-valid member in hjson file")
        pos++
        skip()
        val value = parseValue()
        //Polyloader.log.debug("Got value: \'$value\'")
        data[key] = value
    }

    private fun parseValue(): Any? {
        val unsure = !(next() == '\'' || next() == '\"')
        val value = parseString(true)
        if (unsure)
            when(value) {
                "true" -> return true
                "false" -> return false
                "null" -> return null
            }
        return value
    }

    private fun parseString(parseMultiline : Boolean = false) : String {
        // multiline string
        val startPos = pos
        if (parseMultiline && next() == '\'' && next(1) == '\'' && next(2) == '\'')
            try { return parseMultilineString() }
            catch (_: Exception) { pos = startPos }

        // json-string
        if (next() == '\"' || next() == '\'')
            try { return parseJsonString() }
            catch (_: Exception) { pos = startPos }

        // quoteless string
        try { return parseQuotelessString() }
        catch (e: Exception)
        {
            Polyloader.log.error(e)
            pos = startPos
        }
        error("Could not parse token as string")
    }

    private fun parseMultilineString() : String {
        val sb = StringBuilder("")
        pos += 3 // skip first '''
        while (!eof) {
            if (next() == '\'' && next(1) == '\'' && next(2) == '\'') {
                pos += 3 // skip last '''
                //Polyloader.log.debug("Parsed multiline string: \'$sb\'")
                return sb.toString()
            }
            pos++
            sb.append(next())
        }
        error("Could not parse token as multiline string")
    }

    private fun parseJsonString(): String {
        val quotation = next()
        val sb = StringBuilder("")
        pos++ // skip first quotation
        while (!eof) {
            when (next()) {
                quotation -> {
                    pos++ // skip last quotation
                    //Polyloader.log.debug("Parsed json string: \'$sb\'")
                    return sb.toString()
                }
                '\\' -> when (next(1)) {
                    '\"', '\'', '\\', '/', 'b', 'f', 'n', 'r', 't' -> {
                        sb.append(next(1))
                        pos += 2 // skip \S, where S is a screened character
                    }
                    'u' -> {
                        @Suppress("SpellCheckingInspection") // it doesn't like abcdef
                        val hex = "0123456789abcdef"
                        if (next(2).lowercase() in hex && next(3).lowercase() in hex && next(4).lowercase() in hex && next(5).lowercase() in hex)
                            sb.append(Char(next(2).digitToInt(16) * 1000 + next(3).digitToInt(16) * 100 + next(4).digitToInt(16) * 10 + next(5).digitToInt(16)))
                        else break
                        pos += 6 // skip \uXXXX where X is a HEX numeric
                    }
                }
                else -> { sb.append(next()); pos++ }
            }
        }
        error("Could not parse token as json string")
    }

    private fun parseQuotelessString(expectKey: Boolean = false): String {
        val sb = StringBuilder("")
        when (next()) {
            ',', ':', '[', ']', '{', '}' ->
                error("Found punctuation before a quoteless string")
            else -> {
                sb.append(next())
                pos++
                while (!eof) {
                    if ("\n".contains(next()) || (expectKey && ",:[]{} ".contains(next()))) {
                        if (!expectKey) pos++
                        //Polyloader.log.debug("Parsed quoteless string: $sb")
                        return sb.toString()
                    }
                    sb.append(next())
                    pos++
                }
                return sb.toString()
            }
        }
    }

    private fun skip() {
        while (!eof) {
            when (next()) {
                '\n', '\r', '\t', ' ' -> pos++
                '#' -> skipSingleLineComment() // skips '# hash-type comment'
                '/' ->
                    if (next(1) == '/') { // skips '// double slash -type comment'
                        pos++ // skip extra '/'
                        skipSingleLineComment()
                    }
                    else if (next(1) == '*') // skips '/* multiline\n-type comment */'
                        skipMultiLineComment()
                else -> return
            }
        }
    }

    private fun skipSingleLineComment() {
        pos++ // skip single '/' or '#'
        while (!eof && next() != '\n') // skip until end or line-feed
            pos++
        pos++ // skip '\n' symbol regardless
    }
    private fun skipMultiLineComment() {
        pos += 2 // skip "/*"
        while (!(eof || (next() == '*') && (next(1) == '/')))
            pos++
        pos += 2 // skip "*/"
    }

    // gets next, or next after next, or next after next ... symbol if it exists
    private fun next(offset : Int = 0 ): Char =
        if (pos + offset < hjson.length) hjson[pos + offset] else '\u0000'

    override fun toString(): String {
        return data.toString()
    }
}