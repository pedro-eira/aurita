package aurita.utility.strings

/** Formats a multi-line string.
  *
  */
object FormatString {
  import scala.language.implicitConversions

/** Creates a multi-line string, and replace newlines by given replacement string.
  *
  * @params inputString the input string to process.
  * @params replacementString the replacement character for a newline.
  * @params stripMarginChar the character used by strip margin to create a multi line
  * string.
  * @returns the formatted string
  *
  */
  def stripMarginAndFormat(
    inputString: String,
    replacementString: String = " ",
    stripMarginChar: Char = '|'
  ): String = inputString.stripMargin(stripMarginChar).replaceAll(
    "\n", replacementString
  )

  implicit def formatString(s: String) = new FormatString(s)
}

/** Formats a multi-line string to a single line string.
  *
  */
class FormatString(val s: String) {
  import FormatString.stripMarginAndFormat

  /** Convenient method to create a single line string from a multi-line string with
    * a pipe (i.e. '|') strip margin character.
    *
    */
  def tcombine = stripMarginAndFormat(s)
}
