package org.folio.bulkops.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_CLASSIFICATION;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_CONTRIBUTORS;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_EDITION;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_ELECTRONIC_ACCESS;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_FORMATS;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_LANGUAGES;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_NOTES;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_PHYSICAL_DESCRIPTION;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_PUBLICATION_FREQUENCY;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_PUBLICATION_RANGE;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_RESOURCE_TITLE;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_RESOURCE_TYPE;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_SERIES_STATEMENTS;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_PUBLICATION;
import static org.folio.bulkops.domain.bean.Instance.INSTANCE_SUBJECT;
import static org.folio.bulkops.util.Constants.HYPHEN;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.bulkops.client.MappingRulesClient;
import org.folio.bulkops.domain.dto.BulkOperationMarcRule;
import org.folio.bulkops.domain.dto.BulkOperationMarcRuleCollection;
import org.marc4j.marc.DataField;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
@Log4j2
public class Marc21ReferenceProvider {
  public static final String GENERAL_NOTE = "General note";
  private static final String LOCAL_NOTES = "Local notes";

  private static final Map<String, String> languages = new HashMap<>();
  private static final Map<String, String> classificationTypeNames = new HashMap<>();
  private static Map<String, String> mappedFields = new HashMap<>();

  private final MappingRulesClient mappingRulesClient;

  private final Set<String> noteTags = Set.of("255", "500", "501", "502", "504", "505", "506", "507", "508",
    "510", "511", "513", "514", "515", "516", "518", "520", "521", "522", "524", "525", "526", "530", "532", "533",
    "534", "535", "536", "538", "540", "541", "542", "544", "545", "546", "547", "550", "552", "555", "556", "561",
    "562", "563", "565", "567", "580", "581", "583", "584", "585", "586", "588", "590");
  private final Set<String> localNoteTags = Set.of("591", "592", "593", "594", "595", "596", "597", "598", "599");
  private Map<String, String> instanceNoteTypes = new HashMap<>();
  private Map<String, String> instanceNoteSubfields = new HashMap<>();
  private Set<String> staffOnlyNotes = new HashSet<>();

  static {
    languages.put("aar", "Afar");
    languages.put("abk", "Abkhaz");
    languages.put("ace", "Achinese");
    languages.put("ach", "Acoli");
    languages.put("ada", "Adangme");
    languages.put("ady", "Adygei");
    languages.put("afa", "Afroasiatic (Other)");
    languages.put("afh", "Afrihili (Artificial language)");
    languages.put("afr", "Afrikaans");
    languages.put("ain", "Ainu");
    languages.put("-ajm", "Aljamía");
    languages.put("aka", "Akan");
    languages.put("akk", "Akkadian");
    languages.put("alb", "Albanian");
    languages.put("ale", "Aleut");
    languages.put("alg", "Algonquian (Other)");
    languages.put("alt", "Altai");
    languages.put("amh", "Amharic");
    languages.put("ang", "English, Old (ca. 450-1100)");
    languages.put("anp", "Angika");
    languages.put("apa", "Apache languages");
    languages.put("ara", "Arabic");
    languages.put("arc", "Aramaic");
    languages.put("arg", "Aragonese");
    languages.put("arm", "Armenian");
    languages.put("arn", "Mapuche");
    languages.put("arp", "Arapaho");
    languages.put("art", "Artificial (Other)");
    languages.put("arw", "Arawak");
    languages.put("asm", "Assamese");
    languages.put("ast", "Bable");
    languages.put("ath", "Athapascan (Other)");
    languages.put("aus", "Australian languages");
    languages.put("ava", "Avaric");
    languages.put("ave", "Avestan");
    languages.put("awa", "Awadhi");
    languages.put("aym", "Aymara");
    languages.put("aze", "Azerbaijani");
    languages.put("bad", "Banda languages");
    languages.put("bai", "Bamileke languages");
    languages.put("bak", "Bashkir");
    languages.put("bal", "Baluchi");
    languages.put("bam", "Bambara");
    languages.put("ban", "Balinese");
    languages.put("baq", "Basque");
    languages.put("bas", "Basa");
    languages.put("bat", "Baltic (Other)");
    languages.put("bej", "Beja");
    languages.put("bel", "Belarusian");
    languages.put("bem", "Bemba");
    languages.put("ben", "Bengali");
    languages.put("ber", "Berber (Other)");
    languages.put("bho", "Bhojpuri");
    languages.put("bih", "Bihari (Other)");
    languages.put("bik", "Bikol");
    languages.put("bin", "Edo");
    languages.put("bis", "Bislama");
    languages.put("bla", "Siksika");
    languages.put("bnt", "Bantu (Other)");
    languages.put("bos", "Bosnian");
    languages.put("bra", "Braj");
    languages.put("bre", "Breton");
    languages.put("btk", "Batak");
    languages.put("bua", "Buriat");
    languages.put("bug", "Bugis");
    languages.put("bul", "Bulgarian");
    languages.put("bur", "Burmese");
    languages.put("byn", "Bilin");
    languages.put("cad", "Caddo");
    languages.put("cai", "Central American Indian (Other)");
    languages.put("-cam", "Khmer");
    languages.put("car", "Carib");
    languages.put("cat", "Catalan");
    languages.put("cau", "Caucasian (Other)");
    languages.put("ceb", "Cebuano");
    languages.put("cel", "Celtic (Other)");
    languages.put("cha", "Chamorro");
    languages.put("chb", "Chibcha");
    languages.put("che", "Chechen");
    languages.put("chg", "Chagatai");
    languages.put("chi", "Chinese");
    languages.put("chk", "Chuukese");
    languages.put("chm", "Mari");
    languages.put("chn", "Chinook jargon");
    languages.put("cho", "Choctaw");
    languages.put("chp", "Chipewyan");
    languages.put("chr", "Cherokee");
    languages.put("chu", "Church Slavic");
    languages.put("chv", "Chuvash");
    languages.put("chy", "Cheyenne");
    languages.put("cmc", "Chamic languages");
    languages.put("cnr", "Montenegrin");
    languages.put("cop", "Coptic");
    languages.put("cor", "Cornish");
    languages.put("cos", "Corsican");
    languages.put("cpe", "Creoles and Pidgins, English-based (Other)");
    languages.put("cpf", "Creoles and Pidgins, French-based (Other)");
    languages.put("cpp", "Creoles and Pidgins, Portuguese-based (Other)");
    languages.put("cre", "Cree");
    languages.put("crh", "Crimean Tatar");
    languages.put("crp", "Creoles and Pidgins (Other)");
    languages.put("csb", "Kashubian");
    languages.put("cus", "Cushitic (Other)");
    languages.put("cze", "Czech");
    languages.put("dak", "Dakota");
    languages.put("dan", "Danish");
    languages.put("dar", "Dargwa");
    languages.put("day", "Dayak");
    languages.put("del", "Delaware");
    languages.put("den", "Slavey");
    languages.put("dgr", "Dogrib");
    languages.put("din", "Dinka");
    languages.put("div", "Divehi");
    languages.put("doi", "Dogri");
    languages.put("dra", "Dravidian (Other)");
    languages.put("dsb", "Lower Sorbian");
    languages.put("dua", "Duala");
    languages.put("dum", "Dutch, Middle (ca. 1050-1350)");
    languages.put("dut", "Dutch");
    languages.put("dyu", "Dyula");
    languages.put("dzo", "Dzongkha");
    languages.put("efi", "Efik");
    languages.put("egy", "Egyptian");
    languages.put("eka", "Ekajuk");
    languages.put("elx", "Elamite");
    languages.put("eng", "English");
    languages.put("enm", "English, Middle (1100-1500)");
    languages.put("epo", "Esperanto");
    languages.put("-esk", "Eskimo languages");
    languages.put("-esp", "Esperanto");
    languages.put("est", "Estonian");
    languages.put("-eth", "Ethiopic");
    languages.put("ewe", "Ewe");
    languages.put("ewo", "Ewondo");
    languages.put("fan", "Fang");
    languages.put("fao", "Faroese");
    languages.put("-far", "Faroese");
    languages.put("fat", "Fanti");
    languages.put("fij", "Fijian");
    languages.put("fil", "Filipino");
    languages.put("fin", "Finnish");
    languages.put("fiu", "Finno-Ugrian (Other)");
    languages.put("fon", "Fon");
    languages.put("fre", "French");
    languages.put("-fri", "Frisian");
    languages.put("frm", "French, Middle (ca. 1300-1600)");
    languages.put("fro", "French, Old (ca. 842-1300)");
    languages.put("frr", "North Frisian");
    languages.put("frs", "East Frisian");
    languages.put("fry", "Frisian");
    languages.put("ful", "Fula");
    languages.put("fur", "Friulian");
    languages.put("gaa", "Gã");
    languages.put("-gae", "Scottish Gaelix");
    languages.put("-gag", "Galician");
    languages.put("-gal", "Oromo");
    languages.put("gay", "Gayo");
    languages.put("gba", "Gbaya");
    languages.put("gem", "Germanic (Other)");
    languages.put("geo", "Georgian");
    languages.put("ger", "German");
    languages.put("gez", "Ethiopic");
    languages.put("gil", "Gilbertese");
    languages.put("gla", "Scottish Gaelic");
    languages.put("gle", "Irish");
    languages.put("glg", "Galician");
    languages.put("glv", "Manx");
    languages.put("gmh", "German, Middle High (ca. 1050-1500)");
    languages.put("goh", "German, Old High (ca. 750-1050)");
    languages.put("gon", "Gondi");
    languages.put("gor", "Gorontalo");
    languages.put("got", "Gothic");
    languages.put("grb", "Grebo");
    languages.put("grc", "Greek, Ancient (to 1453)");
    languages.put("gre", "Greek, Modern (1453-)");
    languages.put("grn", "Guarani");
    languages.put("gsw", "Swiss German");
    languages.put("-gua", "Guarani");
    languages.put("guj", "Gujarati");
    languages.put("gwi", "Gwich'in");
    languages.put("hai", "Haida");
    languages.put("hat", "Haitian French Creole");
    languages.put("hau", "Hausa");
    languages.put("haw", "Hawaiian");
    languages.put("heb", "Hebrew");
    languages.put("her", "Herero");
    languages.put("hil", "Hiligaynon");
    languages.put("him", "Western Pahari languages");
    languages.put("hin", "Hindi");
    languages.put("hit", "Hittite");
    languages.put("hmn", "Hmong");
    languages.put("hmo", "Hiri Motu");
    languages.put("hrv", "Croatian");
    languages.put("hsb", "Upper Sorbian");
    languages.put("hun", "Hungarian");
    languages.put("hup", "Hupa");
    languages.put("iba", "Iban");
    languages.put("ibo", "Igbo");
    languages.put("ice", "Icelandic");
    languages.put("ido", "Ido");
    languages.put("iii", "Sichuan Yi");
    languages.put("ijo", "Ijo");
    languages.put("iku", "Inuktitut");
    languages.put("ile", "Interlingue");
    languages.put("ilo", "Iloko");
    languages.put("ina", "Interlingua (International Auxiliary Language Association)");
    languages.put("inc", "Indic (Other)");
    languages.put("ind", "Indonesian");
    languages.put("ine", "Indo-European (Other)");
    languages.put("inh", "Ingush");
    languages.put("-int", "Interlingua (International Auxiliary Language Association)");
    languages.put("ipk", "Inupiaq");
    languages.put("ira", "Iranian (Other)");
    languages.put("-iri", "Irish");
    languages.put("iro", "Iroquoian (Other)");
    languages.put("ita", "Italian");
    languages.put("jav", "Javanese");
    languages.put("jbo", "Lojban (Artificial language)");
    languages.put("jpn", "Japanese");
    languages.put("jpr", "Judeo-Persian");
    languages.put("jrb", "Judeo-Arabic");
    languages.put("kaa", "Kara-Kalpak");
    languages.put("kab", "Kabyle");
    languages.put("kac", "Kachin");
    languages.put("kal", "Kalâtdlisut");
    languages.put("kam", "Kamba");
    languages.put("kan", "Kannada");
    languages.put("kar", "Karen languages");
    languages.put("kas", "Kashmiri");
    languages.put("kau", "Kanuri");
    languages.put("kaw", "Kawi");
    languages.put("kaz", "Kazakh");
    languages.put("kbd", "Kabardian");
    languages.put("kha", "Khasi");
    languages.put("khi", "Khoisan (Other)");
    languages.put("khm", "Khmer");
    languages.put("kho", "Khotanese");
    languages.put("kik", "Kikuyu");
    languages.put("kin", "Kinyarwanda");
    languages.put("kir", "Kyrgyz");
    languages.put("kmb", "Kimbundu");
    languages.put("kok", "Konkani");
    languages.put("kom", "Komi");
    languages.put("kon", "Kongo");
    languages.put("kor", "Korean");
    languages.put("kos", "Kosraean");
    languages.put("kpe", "Kpelle");
    languages.put("krc", "Karachay-Balkar");
    languages.put("krl", "Karelian");
    languages.put("kro", "Kru (Other)");
    languages.put("kru", "Kurukh");
    languages.put("kua", "Kuanyama");
    languages.put("kum", "Kumyk");
    languages.put("kur", "Kurdish");
    languages.put("-kus", "Kusaie");
    languages.put("kut", "Kootenai");
    languages.put("lad", "Ladino");
    languages.put("lah", "Lahndā");
    languages.put("lam", "Lamba (Zambia and Congo)");
    languages.put("-lan", "Occitan (post 1500)");
    languages.put("lao", "Lao");
    languages.put("-lap", "Sami");
    languages.put("lat", "Latin");
    languages.put("lav", "Latvian");
    languages.put("lez", "Lezgian");
    languages.put("lim", "Limburgish");
    languages.put("lin", "Lingala");
    languages.put("lit", "Lithuanian");
    languages.put("lol", "Mongo-Nkundu");
    languages.put("loz", "Lozi");
    languages.put("ltz", "Luxembourgish");
    languages.put("lua", "Luba-Lulua");
    languages.put("lub", "Luba-Katanga");
    languages.put("lug", "Ganda");
    languages.put("lui", "Luiseño");
    languages.put("lun", "Lunda");
    languages.put("luo", "Luo (Kenya and Tanzania)");
    languages.put("lus", "Lushai");
    languages.put("mac", "Macedonian");
    languages.put("mad", "Madurese");
    languages.put("mag", "Magahi");
    languages.put("mah", "Marshallese");
    languages.put("mai", "Maithili");
    languages.put("mak", "Makasar");
    languages.put("mal", "Malayalam");
    languages.put("man", "Mandingo");
    languages.put("mao", "Maori");
    languages.put("map", "Austronesian (Other)");
    languages.put("mar", "Marathi");
    languages.put("mas", "Maasai");
    languages.put("-max", "Manx");
    languages.put("may", "Malay");
    languages.put("mdf", "Moksha");
    languages.put("mdr", "Mandar");
    languages.put("men", "Mende");
    languages.put("mga", "Irish, Middle (ca. 1100-1550)");
    languages.put("mic", "Micmac");
    languages.put("min", "Minangkabau");
    languages.put("mis", "Miscellaneous languages");
    languages.put("mkh", "Mon-Khmer (Other)");
    languages.put("-mla", "Malagasy");
    languages.put("mlg", "Malagasy");
    languages.put("mlt", "Maltese");
    languages.put("mnc", "Manchu");
    languages.put("mni", "Manipuri");
    languages.put("mno", "Manobo languages");
    languages.put("moh", "Mohawk");
    languages.put("-mol", "Moldavian");
    languages.put("mon", "Mongolian");
    languages.put("mos", "Mooré");
    languages.put("mul", "Multiple languages");
    languages.put("mun", "Munda (Other)");
    languages.put("mus", "Creek");
    languages.put("mwl", "Mirandese");
    languages.put("mwr", "Marwari");
    languages.put("myn", "Mayan languages");
    languages.put("myv", "Erzya");
    languages.put("nah", "Nahuatl");
    languages.put("nai", "North American Indian (Other)");
    languages.put("nap", "Neapolitan Italian");
    languages.put("nau", "Nauru");
    languages.put("nav", "Navajo");
    languages.put("nbl", "Ndebele (South Africa)");
    languages.put("nde", "Ndebele (Zimbabwe)");
    languages.put("ndo", "Ndonga");
    languages.put("nds", "Low German");
    languages.put("nep", "Nepali");
    languages.put("new", "Newari");
    languages.put("nia", "Nias");
    languages.put("nic", "Niger-Kordofanian (Other)");
    languages.put("niu", "Niuean");
    languages.put("nno", "Norwegian (Nynorsk)");
    languages.put("nob", "Norwegian (Bokmål)");
    languages.put("nog", "Nogai");
    languages.put("non", "Old Norse");
    languages.put("nor", "Norwegian");
    languages.put("nqo", "N'Ko");
    languages.put("nso", "Northern Sotho");
    languages.put("nub", "Nubian languages");
    languages.put("nwc", "Newari, Old");
    languages.put("nya", "Nyanja");
    languages.put("nym", "Nyamwezi");
    languages.put("nyn", "Nyankole");
    languages.put("nyo", "Nyoro");
    languages.put("nzi", "Nzima");
    languages.put("oci", "Occitan (post-1500)");
    languages.put("oji", "Ojibwa");
    languages.put("ori", "Oriya");
    languages.put("orm", "Oromo");
    languages.put("osa", "Osage");
    languages.put("oss", "Ossetic");
    languages.put("ota", "Turkish, Ottoman");
    languages.put("oto", "Otomian languages");
    languages.put("paa", "Papuan (Other)");
    languages.put("pag", "Pangasinan");
    languages.put("pal", "Pahlavi");
    languages.put("pam", "Pampanga");
    languages.put("pan", "Panjabi");
    languages.put("pap", "Papiamento");
    languages.put("pau", "Palauan");
    languages.put("peo", "Old Persian (ca. 600-400 B.C.)");
    languages.put("per", "Persian");
    languages.put("phi", "Philippine (Other)");
    languages.put("phn", "Phoenician");
    languages.put("pli", "Pali");
    languages.put("pol", "Polish");
    languages.put("pon", "Pohnpeian");
    languages.put("por", "Portuguese");
    languages.put("pra", "Prakrit languages");
    languages.put("pro", "Provençal (to 1500)");
    languages.put("pus", "Pushto");
    languages.put("que", "Quechua");
    languages.put("raj", "Rajasthani");
    languages.put("rap", "Rapanui");
    languages.put("rar", "Rarotongan");
    languages.put("roa", "Romance (Other)");
    languages.put("roh", "Raeto-Romance");
    languages.put("rom", "Romani");
    languages.put("rum", "Romanian");
    languages.put("run", "Rundi");
    languages.put("rup", "Aromanian");
    languages.put("rus", "Russian");
    languages.put("sad", "Sandawe");
    languages.put("sag", "Sango (Ubangi Creole)");
    languages.put("sah", "Yakut");
    languages.put("sai", "South American Indian (Other)");
    languages.put("sal", "Salishan languages");
    languages.put("sam", "Samaritan Aramaic");
    languages.put("san", "Sanskrit");
    languages.put("-sao", "Samoan");
    languages.put("sas", "Sasak");
    languages.put("sat", "Santali");
    languages.put("-scc", "Serbian");
    languages.put("scn", "Sicilian Italian");
    languages.put("sco", "Scots");
    languages.put("-scr", "Croatian");
    languages.put("sel", "Selkup");
    languages.put("sem", "Semitic (Other)");
    languages.put("sga", "Irish, Old (to 1100)");
    languages.put("sgn", "Sign languages");
    languages.put("shn", "Shan");
    languages.put("-sho", "Shona");
    languages.put("sid", "Sidamo");
    languages.put("sin", "Sinhalese");
    languages.put("sio", "Siouan (Other)");
    languages.put("sit", "Sino-Tibetan (Other)");
    languages.put("sla", "Slavic (Other)");
    languages.put("slo", "Slovak");
    languages.put("slv", "Slovenian");
    languages.put("sma", "Southern Sami");
    languages.put("sme", "Northern Sami");
    languages.put("smi", "Sami");
    languages.put("smj", "Lule Sami");
    languages.put("smn", "Inari Sami");
    languages.put("smo", "Samoan");
    languages.put("sms", "Skolt Sami");
    languages.put("sna", "Shona");
    languages.put("snd", "Sindhi");
    languages.put("-snh", "Sinhalese");
    languages.put("snk", "Soninke");
    languages.put("sog", "Sogdian");
    languages.put("som", "Somali");
    languages.put("son", "Songhai");
    languages.put("sot", "Sotho");
    languages.put("spa", "Spanish");
    languages.put("srd", "Sardinian");
    languages.put("srn", "Sranan");
    languages.put("srp", "Serbian");
    languages.put("srr", "Serer");
    languages.put("ssa", "Nilo-Saharan (Other)");
    languages.put("-sso", "Sotho");
    languages.put("ssw", "Swazi");
    languages.put("suk", "Sukuma");
    languages.put("sun", "Sundanese");
    languages.put("sus", "Susu");
    languages.put("sux", "Sumerian");
    languages.put("swa", "Swahili");
    languages.put("swe", "Swedish");
    languages.put("-swz", "Swazi");
    languages.put("syc", "Syriac");
    languages.put("syr", "Syriac, Modern");
    languages.put("-tag", "Tagalog");
    languages.put("tah", "Tahitian");
    languages.put("tai", "Tai (Other)");
    languages.put("-taj", "Tajik");
    languages.put("tam", "Tamil");
    languages.put("-tar", "Tatar");
    languages.put("tat", "Tatar");
    languages.put("tel", "Telugu");
    languages.put("tem", "Temne");
    languages.put("ter", "Terena");
    languages.put("tet", "Tetum");
    languages.put("tgk", "Tajik");
    languages.put("tgl", "Tagalog");
    languages.put("tha", "Thai");
    languages.put("tib", "Tibetan");
    languages.put("tig", "Tigré");
    languages.put("tir", "Tigrinya");
    languages.put("tiv", "Tiv");
    languages.put("tkl", "Tokelauan");
    languages.put("tlh", "Klingon (Artificial language)");
    languages.put("tli", "Tlingit");
    languages.put("tmh", "Tamashek");
    languages.put("tog", "Tonga (Nyasa)");
    languages.put("ton", "Tongan");
    languages.put("tpi", "Tok Pisin");
    languages.put("-tru", "Truk");
    languages.put("tsi", "Tsimshian");
    languages.put("tsn", "Tswana");
    languages.put("tso", "Tsonga");
    languages.put("-tsw", "Tswana");
    languages.put("tuk", "Turkmen");
    languages.put("tum", "Tumbuka");
    languages.put("tup", "Tupi languages");
    languages.put("tur", "Turkish");
    languages.put("tut", "Altaic (Other)");
    languages.put("tvl", "Tuvaluan");
    languages.put("twi", "Twi");
    languages.put("tyv", "Tuvinian");
    languages.put("udm", "Udmurt");
    languages.put("uga", "Ugaritic");
    languages.put("uig", "Uighur");
    languages.put("ukr", "Ukrainian");
    languages.put("umb", "Umbundu");
    languages.put("und", "Undetermined");
    languages.put("urd", "Urdu");
    languages.put("uzb", "Uzbek");
    languages.put("vai", "Vai");
    languages.put("ven", "Venda");
    languages.put("vie", "Vietnamese");
    languages.put("vol", "Volapük");
    languages.put("vot", "Votic");
    languages.put("wak", "Wakashan languages");
    languages.put("wal", "Wolayta");
    languages.put("war", "Waray");
    languages.put("was", "Washoe");
    languages.put("wel", "Welsh");
    languages.put("wen", "Sorbian (Other)");
    languages.put("wln", "Walloon");
    languages.put("wol", "Wolof");
    languages.put("xal", "Oirat");
    languages.put("xho", "Xhosa");
    languages.put("yao", "Yao (Africa)");
    languages.put("yap", "Yapese");
    languages.put("yid", "Yiddish");
    languages.put("yor", "Yoruba");
    languages.put("ypk", "Yupik languages");
    languages.put("zap", "Zapotec");
    languages.put("zbl", "Blissymbolics");
    languages.put("zen", "Zenaga");
    languages.put("zha", "Zhuang");
    languages.put("znd", "Zande languages");
    languages.put("zul", "Zulu");
    languages.put("zun", "Zuni");
    languages.put("zxx", "No linguistic content");
    languages.put("zza", "Zaza");

    mappedFields.put("041", INSTANCE_LANGUAGES);
    List.of("050", "060", "080", "082", "086", "090")
      .forEach(tag -> mappedFields.put(tag, INSTANCE_CLASSIFICATION));
    List.of("100", "110", "111", "700", "710", "711", "720")
      .forEach(tag -> mappedFields.put(tag, INSTANCE_CONTRIBUTORS));
    mappedFields.put("245", INSTANCE_RESOURCE_TITLE);
    mappedFields.put("250", INSTANCE_EDITION);
    mappedFields.put("300", INSTANCE_PHYSICAL_DESCRIPTION);
    List.of("310", "321").forEach(tag -> mappedFields.put(tag, INSTANCE_PUBLICATION_FREQUENCY));
    mappedFields.put("336", INSTANCE_RESOURCE_TYPE);
    mappedFields.put("338", INSTANCE_FORMATS);
    mappedFields.put("362", INSTANCE_PUBLICATION_RANGE);
    List.of("600", "610", "611", "630", "647", "648", "650", "651", "655")
        .forEach(tag -> mappedFields.put(tag, INSTANCE_SUBJECT));
    List.of("800", "810", "811", "830")
      .forEach(tag -> mappedFields.put(tag, INSTANCE_SERIES_STATEMENTS));
    mappedFields.put("856", INSTANCE_ELECTRONIC_ACCESS);
    List.of("260", "264").forEach(tag -> mappedFields.put(tag, INSTANCE_PUBLICATION));

    classificationTypeNames.put("050", "LC");
    classificationTypeNames.put("060", "NLM");
    classificationTypeNames.put("080", "UDC");
    classificationTypeNames.put("082", "Dewey");
    classificationTypeNames.put("086", "GDC");
    classificationTypeNames.put("090", "LC");
  }

  public void updateMappingRules() {
    var documentContext = JsonPath.parse(mappingRulesClient.getMarcBibMappingRules());

    instanceNoteTypes = new HashMap<>();
    instanceNoteSubfields = new HashMap<>();
    staffOnlyNotes = new HashSet<>();

    noteTags.forEach(tag -> {
      instanceNoteTypes.put(tag, fetchNoteType(documentContext, tag));
      instanceNoteSubfields.put(tag, fetchSubfields(documentContext, tag));
      if (isStaffOnly(documentContext, tag)) {
        staffOnlyNotes.add(tag);
      }
    });

    localNoteTags.forEach(tag -> {
      instanceNoteTypes.put(tag, instanceNoteTypes.getOrDefault(tag, LOCAL_NOTES));
      instanceNoteSubfields.put(tag, instanceNoteSubfields.getOrDefault(tag, "a"));
    });
  }

  private String fetchNoteType(DocumentContext context, String tag) {
    var path = String.format("$..%s[*].entity[*].rules[*].conditions[?(@.type == 'set_note_type_id')].parameter.name", tag);
    var res = context.read(path, List.class);
    return res.isEmpty() ? EMPTY : res.get(0).toString();
  }

  private String fetchSubfields(DocumentContext context, String tag) {
    var path = String.format("$..%s[*].entity[?(@.target == 'notes.note')].subfield[*]", tag);
    var res = context.read(path, List.class);
    return IntStream.range(0, res.size())
      .mapToObj(i -> res.get(i).toString())
      .collect(Collectors.joining(EMPTY));
  }

  private boolean isStaffOnly(DocumentContext context, String tag) {
    var path = String.format("$..%s[*].entity[?(@.target == 'notes.staffOnly')].rules[*].conditions[*].type", tag);
    var res = context.read(path, List.class);
    return !res.isEmpty() && "set_note_staff_only_via_indicator".equals(res.get(0));
  }

  public String getLanguageByCode(String code) {
    return languages.getOrDefault(code, code);
  }

  public String getNoteTypeByTag(String tag) {
    return instanceNoteTypes.getOrDefault(tag, GENERAL_NOTE);
  }

  public String getSubfieldsByTag(String tag) {
    return instanceNoteSubfields.getOrDefault(tag, "a");
  }

  public boolean isStaffOnlyNote(DataField dataField) {
    return staffOnlyNotes.contains(dataField.getTag()) && '0' == dataField.getIndicator1();
  }

  public boolean isMappedTag(String tag) {
    return mappedFields.containsKey(tag) || noteTags.contains(tag);
  }

  public boolean isMappedNoteTag(String tag) {
    return noteTags.contains(tag);
  }

  public String getFieldNameByTag(String tag) {
    return noteTags.contains(tag) ? instanceNoteTypes.getOrDefault(tag, GENERAL_NOTE) : mappedFields.get(tag);
  }

  public String getFieldNameByTagForCsv(String tag) {
    return noteTags.contains(tag) ? INSTANCE_NOTES : mappedFields.get(tag);
  }

  public Set<String> getChangedOptionsSet(BulkOperationMarcRuleCollection rules) {
    return rules.getBulkOperationMarcRules().stream()
      .map(BulkOperationMarcRule::getTag)
      .filter(this::isMappedTag)
      .map(this::getFieldNameByTag)
      .collect(Collectors.toSet());
  }

  public Set<String> getChangedOptionsSetForCsv(BulkOperationMarcRuleCollection rules) {
    return rules.getBulkOperationMarcRules().stream()
      .map(BulkOperationMarcRule::getTag)
      .filter(this::isMappedTag)
      .map(this::getFieldNameByTagForCsv)
      .collect(Collectors.toSet());
  }

  public String getClassificationTypeByTag(String tag) {
    return classificationTypeNames.getOrDefault(tag, HYPHEN);
  }
}
