// $ANTLR 2.7.7 (2006-11-01): "XQueryTree.g" -> "XQueryTreeParser.java"$

	package org.exist.xquery.parser;

	import antlr.debug.misc.*;
	import java.io.StringReader;
	import java.io.BufferedReader;
	import java.io.InputStreamReader;
	import java.util.ArrayList;
	import java.util.List;
	import java.util.Iterator;
	import java.util.Map;
	import java.util.Set;
	import java.util.TreeSet;
	import java.util.HashMap;
	import java.util.Stack;
	import javax.xml.XMLConstants;
	import org.exist.storage.BrokerPool;
	import org.exist.storage.DBBroker;
	import org.exist.EXistException;
	import org.exist.Namespaces;
	import org.exist.dom.persistent.DocumentSet;
	import org.exist.dom.persistent.DocumentImpl;
	import org.exist.dom.QName;
	import org.exist.security.PermissionDeniedException;
	import org.exist.util.XMLChar;
	import org.exist.xquery.*;
	import org.exist.xquery.Constants.Comparison;
	import org.exist.xquery.Constants.NodeComparisonOperator;
	import org.exist.xquery.value.*;
	import org.exist.xquery.functions.fn.*;
	import org.exist.xquery.update.*;
	import org.exist.storage.ElementValue;
	import org.exist.xquery.functions.map.MapExpr;
	import org.exist.xquery.functions.array.ArrayConstructor;

public interface XQueryTreeParserTokenTypes {
	int EOF = 1;
	int NULL_TREE_LOOKAHEAD = 3;
	int QNAME = 4;
	int EQNAME = 5;
	int PREDICATE = 6;
	int FLWOR = 7;
	int PARENTHESIZED = 8;
	int ABSOLUTE_SLASH = 9;
	int ABSOLUTE_DSLASH = 10;
	int WILDCARD = 11;
	int PREFIX_WILDCARD = 12;
	int FUNCTION = 13;
	int DYNAMIC_FCALL = 14;
	int UNARY_MINUS = 15;
	int UNARY_PLUS = 16;
	int XPOINTER = 17;
	int XPOINTER_ID = 18;
	int VARIABLE_REF = 19;
	int VARIABLE_BINDING = 20;
	int ELEMENT = 21;
	int ATTRIBUTE = 22;
	int ATTRIBUTE_CONTENT = 23;
	int TEXT = 24;
	int VERSION_DECL = 25;
	int NAMESPACE_DECL = 26;
	int DEF_NAMESPACE_DECL = 27;
	int DEF_COLLATION_DECL = 28;
	int DEF_FUNCTION_NS_DECL = 29;
	int ANNOT_DECL = 30;
	int GLOBAL_VAR = 31;
	int FUNCTION_DECL = 32;
	int INLINE_FUNCTION_DECL = 33;
	int FUNCTION_INLINE = 34;
	int FUNCTION_TEST = 35;
	int MAP_TEST = 36;
	int LOOKUP = 37;
	int ARRAY = 38;
	int ARRAY_TEST = 39;
	int PROLOG = 40;
	int OPTION = 41;
	int ATOMIC_TYPE = 42;
	int MODULE = 43;
	int ORDER_BY = 44;
	int GROUP_BY = 45;
	int POSITIONAL_VAR = 46;
	int CATCH_ERROR_CODE = 47;
	int CATCH_ERROR_DESC = 48;
	int CATCH_ERROR_VAL = 49;
	int MODULE_DECL = 50;
	int MODULE_IMPORT = 51;
	int SCHEMA_IMPORT = 52;
	int ATTRIBUTE_TEST = 53;
	int COMP_ELEM_CONSTRUCTOR = 54;
	int COMP_ATTR_CONSTRUCTOR = 55;
	int COMP_TEXT_CONSTRUCTOR = 56;
	int COMP_COMMENT_CONSTRUCTOR = 57;
	int COMP_PI_CONSTRUCTOR = 58;
	int COMP_NS_CONSTRUCTOR = 59;
	int COMP_DOC_CONSTRUCTOR = 60;
	int PRAGMA = 61;
	int GTEQ = 62;
	int SEQUENCE = 63;
	int LITERAL_xpointer = 64;
	int LPAREN = 65;
	int RPAREN = 66;
	int NCNAME = 67;
	int LITERAL_xquery = 68;
	int LITERAL_version = 69;
	int SEMICOLON = 70;
	int LITERAL_module = 71;
	int LITERAL_namespace = 72;
	int EQ = 73;
	int STRING_LITERAL = 74;
	int LITERAL_declare = 75;
	int LITERAL_default = 76;
	// "boundary-space" = 77
	int LITERAL_ordering = 78;
	int LITERAL_construction = 79;
	// "base-uri" = 80
	// "copy-namespaces" = 81
	int LITERAL_option = 82;
	int LITERAL_function = 83;
	int LITERAL_variable = 84;
	int MOD = 85;
	int LITERAL_import = 86;
	int LITERAL_encoding = 87;
	int LITERAL_collation = 88;
	int LITERAL_element = 89;
	int LITERAL_order = 90;
	int LITERAL_empty = 91;
	int LITERAL_greatest = 92;
	int LITERAL_least = 93;
	int LITERAL_preserve = 94;
	int LITERAL_strip = 95;
	int LITERAL_ordered = 96;
	int LITERAL_unordered = 97;
	int COMMA = 98;
	// "no-preserve" = 99
	int LITERAL_inherit = 100;
	// "no-inherit" = 101
	int DOLLAR = 102;
	int LCURLY = 103;
	int RCURLY = 104;
	int COLON = 105;
	int LITERAL_external = 106;
	int LITERAL_schema = 107;
	int BRACED_URI_LITERAL = 108;
	int LITERAL_as = 109;
	int LITERAL_at = 110;
	// "empty-sequence" = 111
	int QUESTION = 112;
	int STAR = 113;
	int PLUS = 114;
	int LITERAL_item = 115;
	int LITERAL_map = 116;
	int LITERAL_array = 117;
	int LITERAL_for = 118;
	int LITERAL_let = 119;
	int LITERAL_try = 120;
	int LITERAL_some = 121;
	int LITERAL_every = 122;
	int LITERAL_if = 123;
	int LITERAL_switch = 124;
	int LITERAL_typeswitch = 125;
	int LITERAL_update = 126;
	int LITERAL_replace = 127;
	int LITERAL_value = 128;
	int LITERAL_insert = 129;
	int LITERAL_delete = 130;
	int LITERAL_rename = 131;
	int LITERAL_with = 132;
	int LITERAL_into = 133;
	int LITERAL_preceding = 134;
	int LITERAL_following = 135;
	int LITERAL_catch = 136;
	int UNION = 137;
	int LITERAL_return = 138;
	int LITERAL_where = 139;
	int LITERAL_in = 140;
	int LITERAL_allowing = 141;
	int LITERAL_by = 142;
	int LITERAL_stable = 143;
	int LITERAL_ascending = 144;
	int LITERAL_descending = 145;
	int LITERAL_group = 146;
	int LITERAL_satisfies = 147;
	int LITERAL_case = 148;
	int LITERAL_then = 149;
	int LITERAL_else = 150;
	int LITERAL_or = 151;
	int LITERAL_and = 152;
	int LITERAL_instance = 153;
	int LITERAL_of = 154;
	int LITERAL_treat = 155;
	int LITERAL_castable = 156;
	int LITERAL_cast = 157;
	int BEFORE = 158;
	int AFTER = 159;
	int LITERAL_eq = 160;
	int LITERAL_ne = 161;
	int LITERAL_lt = 162;
	int LITERAL_le = 163;
	int LITERAL_gt = 164;
	int LITERAL_ge = 165;
	int GT = 166;
	int NEQ = 167;
	int LT = 168;
	int LTEQ = 169;
	int LITERAL_is = 170;
	int LITERAL_isnot = 171;
	int CONCAT = 172;
	int LITERAL_to = 173;
	int MINUS = 174;
	int LITERAL_div = 175;
	int LITERAL_idiv = 176;
	int LITERAL_mod = 177;
	int BANG = 178;
	int PRAGMA_START = 179;
	int PRAGMA_END = 180;
	int LITERAL_union = 181;
	int LITERAL_intersect = 182;
	int LITERAL_except = 183;
	int SLASH = 184;
	int DSLASH = 185;
	int LITERAL_text = 186;
	int LITERAL_node = 187;
	int LITERAL_attribute = 188;
	int LITERAL_comment = 189;
	// "processing-instruction" = 190
	// "document-node" = 191
	int LITERAL_document = 192;
	int HASH = 193;
	int SELF = 194;
	int XML_COMMENT = 195;
	int XML_PI = 196;
	int LPPAREN = 197;
	int RPPAREN = 198;
	int AT = 199;
	int PARENT = 200;
	int LITERAL_child = 201;
	int LITERAL_self = 202;
	int LITERAL_descendant = 203;
	// "descendant-or-self" = 204
	// "following-sibling" = 205
	int LITERAL_parent = 206;
	int LITERAL_ancestor = 207;
	// "ancestor-or-self" = 208
	// "preceding-sibling" = 209
	int INTEGER_LITERAL = 210;
	int DOUBLE_LITERAL = 211;
	int DECIMAL_LITERAL = 212;
	// "schema-element" = 213
	int END_TAG_START = 214;
	int QUOT = 215;
	int APOS = 216;
	int QUOT_ATTRIBUTE_CONTENT = 217;
	int ESCAPE_QUOT = 218;
	int APOS_ATTRIBUTE_CONTENT = 219;
	int ESCAPE_APOS = 220;
	int ELEMENT_CONTENT = 221;
	int XML_COMMENT_END = 222;
	int XML_PI_END = 223;
	int XML_CDATA = 224;
	int LITERAL_collection = 225;
	int LITERAL_validate = 226;
	int XML_PI_START = 227;
	int XML_CDATA_START = 228;
	int XML_CDATA_END = 229;
	int LETTER = 230;
	int DIGITS = 231;
	int HEX_DIGITS = 232;
	int NMSTART = 233;
	int NMCHAR = 234;
	int WS = 235;
	int XQDOC_COMMENT = 236;
	int EXPR_COMMENT = 237;
	int PREDEFINED_ENTITY_REF = 238;
	int CHAR_REF = 239;
	int S = 240;
	int NEXT_TOKEN = 241;
	int CHAR = 242;
	int BASECHAR = 243;
	int IDEOGRAPHIC = 244;
	int COMBINING_CHAR = 245;
	int DIGIT = 246;
	int EXTENDER = 247;
	int INTEGER_VALUE = 248;
}
