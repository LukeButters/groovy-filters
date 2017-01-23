package filter

import com.funnelback.common.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

@groovy.transform.InheritConstructors
public class CAExtraFilters extends com.funnelback.common.filter.ScriptFilterProvider {

	// Insert metatag before closing tag <head>
	private void insertMetatag(Document doc, String name, String content) {
		doc.head().appendElement("meta").attr("name", name).attr("content", content)
	}

	// Helpers
	private Boolean isMethod(String method) {
		return CAExtraFilters.metaClass.respondsTo(CAExtraFilters, method)
	}

	private String resolveSign(sign) {
		switch(sign) {
			case '>': return 'Gt'; break
			case '<': return 'Lt'; break
			case '=': return 'Eq'; break
			default: return null
		}
	}

	private Integer wordsCount(String str) {
		return str.tokenize().size()
	} 

	// Check string length
	private Boolean cLengthEq(String str, Integer length) {
		return str.length() == length
	}

	private Boolean cLengthGt(String str, Integer length) {
		return str.length() > length
	}

	private Boolean cLengthLt(String str, Integer length) {
		return str.length() < length
	}

	private Boolean wLengthEq(String str, Integer length) {
		return wordsCount(str) == length
	}

	private Boolean wLengthGt(String str, Integer length) {
		return wordsCount(str) > length
	}

	private Boolean wLengthLt(String str, Integer length) {
		return wordsCount(str) < length
	}

	private void checkElementLength(File file, Document doc) {
		def content = [:], key, data, met, sign, type
		Elements els

		file.eachLine {
			if (it.startsWith('#')) return
			if (!it.length()) return

			data = it.tokenize(',')
			if (data.size < 3) return

			els = doc.select(data[0])
			if (!els.size()) {
				key = 'FunElementExist0'
				if (!content[key]) content[key] = []
				content[key] << data[0]
				return
			}

			type = data[1]
			sign = resolveSign(data[2])
			key  = 'FunElementLength' + type.capitalize() + sign + data[3]
			if (!content[key]) content[key] = []

			met = "$type"+"Length$sign"
			if (isMethod(met)) {
				for (Element el : els) {
					if("$met"(el.text(), data[3].toInteger())) content[key] << data[0]
				}
			}
		}

		content.each {k, v ->
			if (v.size) insertMetatag(doc, k, v.join('|'))
		}
	}

	// Check element content
	private Boolean endWith(String str, String substr) {
		return str.endsWith(substr)
	}

	private Boolean equal(String str1, String str2) {
		return str1 == str2
	}

	private Boolean has(String str, String substr) {
		return str.contains(substr)
	}

	private Boolean startWith(String str, String substr) {
		return str.startsWith(substr)
	}

	private Boolean notEndWith(String str, String substr) {
		return !endWith(str, substr)
	}

	private Boolean notEqual(String str1, String str2) {
		return !equal(str1, str2)
	}

	private Boolean notHas(String str, String substr) {
		return !has(str, substr)
	}

	private Boolean notStartWith(String str, String substr) {
		return !startWith(str, substr)
	}

	private void checkElementContent(File file, Document doc) {
		def content = [:], data, met, key, val, ret
		Element el

		file.eachLine {
			if (it.startsWith('#')) return
			if (!it.length()) return

			data = it.tokenize(',')
			if (data.size < 3) return

			el  = doc.select(data[0]).first()
			if (!el) {
				key = 'FunElementExist0'
				if (!content[key]) content[key] = []
				content[key] << data[0]
				return
			}

			met = data[1]
			key = 'FunElement' + data[0].capitalize().replaceAll(/\s/, "") + met.capitalize()

			if (data[2].startsWith('"')) {
				val = data[2].substring(1, data[2].length() - 1)
				ret = val
			}
			else {
				val = doc.select(data[2]).first() ? doc.select(data[2]).first().text() : ''
				ret = data[2]
			}

			if (!content[key]) content[key] = []
			if (isMethod(met) && "$met"(el.text(), val)) content[key] << ret
		}

		content.each {k, v ->
			if (v.size) insertMetatag(doc, k, v.join('|'))
		}
	}

	// Check if element exist
	private void existElement(File file, Document doc) {
		def content = [:], key, ret
		Elements els

		file.eachLine {
			if (it.startsWith('#')) return
			if (!it.length()) return

			els = doc.select(it)

 			if(!els.size()) {
 				key = 'FunElementExist0'
 				ret = it
 			}
 			else {
				key = 'FunElementExist' + it.replaceAll(/\s/, "").capitalize()
				ret = els.size()
			}

			if (!content[key]) content[key] = []
			content[key] << ret
		}

		content.each {k, v ->
			if (v.size) insertMetatag(doc, k, v.join('|'))
		}
	}

	// We filter all documents
	public Boolean isDocumentFilterable(String documentType) {
		return true;
	}

	public String filter(String input, String documentType) {
		Document doc = Jsoup.parse(input)
		def file, filePath

		filePath = config.value('ui.ca.exist_element')
		if (filePath) {
			file = new File(filePath)
			if (file.exists()) existElement(file, doc)
		}

		filePath = config.value('ui.ca.check_element_length')
		if (filePath) {
			file = new File(filePath)
			if (file.exists()) checkElementLength(file, doc)
		}

		filePath = config.value('ui.ca.check_element_content')
		if (filePath) {
			file = new File(filePath)
			if (file.exists()) checkElementContent(file, doc)
		}

		return doc.outerHtml()
	}

	// A main method to allow very basic testing
	public static void main(String[] args) {
		if (args.length < 2) { println 'Missing arguments: <collection_name> <file_path>'; return }

		def f    = new CAExtraFilters(args[0], false);
		def file = new File(args[1])
		if (file.exists()) {
			def ext = args[1].substring(args[1].lastIndexOf('.') + 1)
			println f.filter(file.getText(), ext)
		}
	}
}