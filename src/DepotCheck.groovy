/**
* ToDo:
 * OK 1. Performance seit Kauf erg�nzen
 * OK 2. absoluten G/V erg�nzen
 * OK 3. Kauftag erg�nzen
 * 4. Alter erg�nzen
 * 5. Ausgabe aufr�umen, Zahlenformat nicht 10stellig, nur soviele Stellen, wie n�tig
 * 6. Ausgabe alternativ als HTML-Tabelle
 * 7. Link auf WP bei comdirect anh�ngen
* Auf http://www.finanzen.net/kurse/kurse_historisch.asp gibt es bessere historische Kurse, die enden nicht drei Tage vorher!
* 
* Requirements:
* parallel zu src werden diese Files ben�tigt:
* css/theme.blue.css
* lib/jquery-2.0.3.min.js
* lib/jquery.tablesorter.min.js
* lib/jquery.tablesorter.widgets.min.js
*/


import java.text.NumberFormat;
import java.text.SimpleDateFormat
import org.cyberneko.html.parsers.SAXParser

import java.time.LocalDate
import java.time.format.DateTimeFormatter;

public class DepotCheck {

	final int IGNORE_LAST_DAYS = 5
	final int DAYS_TO_NEXT_MIN = 2
    static final String DEL =";"
	double NEAR = 0.015
	int MONTH = 6
	int MIN_DISTANCE = 90
	int SIGNIFICANT_DISTANCE = (MONTH*30.5)/6
	int INCREMENTS = 300

	int relevant = StockValue.CLOSE

    public static DateTimeFormatter stdFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    public static DateTimeFormatter sortFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private static String HISTORICAL_URL ="https://www.comdirect.de/inf/kursdaten/historic.csv?DATETIME_TZ_START_RANGE_FORMATED=#startDate&ID_NOTATION=#notation&mask=true&INTERVALL=16&OFFSET=#offset&modal=false&DATETIME_TZ_END_RANGE_FORMATED=#endDate"

	ArrayList<OutputInfo> upTrendNears = new ArrayList<OutputInfo>()
	ArrayList<OutputInfo> upTrendNotNears = new ArrayList<OutputInfo>()
	ArrayList<OutputInfo> downTrendNears = new ArrayList<OutputInfo>()
	ArrayList<OutputInfo> downTrendNotNears = new ArrayList<OutputInfo>()
	ArrayList<OutputInfo> noTrends = new ArrayList<OutputInfo>()
	ArrayList<OutputInfo> problems = new ArrayList<OutputInfo>()

	StockValue importLine(line, lineCount, relevant) {
		if (lineCount == 0) {
			return
		}
		StockValue value = new StockValue(line, relevant)
		return value
	}

	double fetchCurrentPrice(String comdNotationId) {
		println comdNotationId
		def html = ""
		def currentPriceUrl =""
		println "getting current price..."
		def price = ""
			currentPriceUrl = "https://www.comdirect.de/inf/aktien/detail/uebersicht.html?INDEX_FILTER=true&ID_NOTATION="+comdNotationId
	                  
			println currentPriceUrl
			html = new XmlSlurper(new SAXParser()).parse(currentPriceUrl)
			price = html.'**'.findAll {
				it.@class.text()=="price"
			}[0].toString()
			println "COMDIRECT price:" + price
		
		if (price == null || price.equals("null")) {
		try { price = html.'**'.findAll {
				it.@id.text()=="yfs_l10_"+symbol.toLowerCase()
			}[0].toString()
			println "Current value from yfs_110"
		}
		catch(Exception ex)
		{
			println "Seltsames HTML: $html"
		}

		}
		NumberFormat nf = NumberFormat.getInstance(Locale.GERMAN)
		println "Price: $price"
    try{
		
    		return nf.parse(price)
    }
    catch(java.text.ParseException ex)
    { 
      return 0
    }
        
	}

	String createUrl(String symbol, int days){
		// M d Y
		// a=02 b=01 c=2010 d=11 e=14 f=2010
		// von 01.03.2010
		// bis 14.12.2010
		Date today = new Date()
		Date halfYearAgo = today.minus(days)

		int a = halfYearAgo.getAt(Calendar.MONTH)
		int b = halfYearAgo.getAt(Calendar.DAY_OF_MONTH)
		int c = halfYearAgo.getAt(Calendar.YEAR)

		int d = today.getAt(Calendar.MONTH)
		int e = today.getAt(Calendar.DAY_OF_MONTH)
		int f = today.getAt(Calendar.YEAR)

		String url = "https://ichart.finance.yahoo.com/table.csv?s="+symbol.toUpperCase()+"&a=$a&b=$b&c=$c&d=$d&e=$e&f=$f&g=d&ignore=.csv"
		return url
	}

    void importHistoricalData(ArrayList<Security> securities)
    {    // read from:
        // http://www.comdirect.de/inf/kursdaten/historic.csv?DATETIME_TZ_START_RANGE_FORMATED=11.06.2010&ID_NOTATION=3240907&mask=true&INTERVALL=16&OFFSET=2&modal=false&DATETIME_TZ_END_RANGE_FORMATED=11.06.2015

		for (Security security : securities) {
            String urlWithOffset = HISTORICAL_URL.replace("#notation", security.getComdNotationId()+"")
            LocalDate now = LocalDate.now()
             println "Java8 Date formatting: " + stdFormat.format(now);
            LocalDate fiveYearsAgo = now.minusYears(5);
            urlWithOffset = urlWithOffset.replace("#startDate", stdFormat.format(fiveYearsAgo))
            urlWithOffset = urlWithOffset.replace("#endDate", stdFormat.format(now))
            int offset = 0
            boolean running = true
            while (running) {
                println "offset: "+offset +" WKN:"+security.wkn+" "+security.name
                String url = urlWithOffset.replace("#offset", Integer.toString(offset))
                String data;
                try {
                    data = url.toURL().text
                }
                catch(FileNotFoundException ex)
                {
                    running = false
                }

                if (!running)
                {
                    break
                }
                String[] lines = data.split("\n")
                for (int i = 0; i < lines.length; i++) {
                    if (lines[i].count(";") < 5 || lines[i].length() > 5 && !lines[i].substring(0, 1).isNumber()) {
                        continue;
                    }
                    println(i + " " + lines[i])
                    String[] dataset = lines[i].split(";")
                    String dateString = dataset[0];
					if (dataset[4]!=null && dataset[4].length() > 1) {
						dataset[4] = dataset[4].replaceAll("\\.", "");
					}
                    Float close = Float.parseFloat(dataset[4].replaceAll(",", "."));

                    // lines start this way:
                    //0
                    //1 ISHARES TECDAX (DE)(WKN: 593397 B�rse: LT Lang & Schwarz)
                    //2
                    //3 Datum;Er�ffnung;Hoch;Tief;Schluss;Volumen
                    //4 13.02.2015;14,384;14,389;14,192;14,235;0,00

                    println(i + " " + dateString + ":" + close)
                    LocalDate date = LocalDate.parse(dateString,stdFormat);
                    String sortDate = sortFormat.format(date)
                    security.historicalData.put(sortDate, close);
                    security.firstDate = sortFormat.format(date);
                }
                offset++
            }
        }
    }

	void importStocks(ArrayList<Security> stocks, File stocksFile){
		stocksFile.eachLine { line ->
			String[] lineValues = line.tokenize(';');
			if (lineValues.length > 1 && !lineValues[0].equalsIgnoreCase("WKN")) {
				Security stock = new Security(lineValues);
				stocks << stock;
			}
		}
	}

	double trendDiff(double lastTrendValue, double currentPrice){
		double diff = (1-lastTrendValue / currentPrice)
		return diff
	}
	
	boolean isNear(double diff){
		return diff.abs() < NEAR
	}
	
	double getLastTrendValue(Line trend, int days){
		println trend
		return trend.getY(days)
	}

	ArrayList<StockValue> importStockValues(Security stock, URL url){
		ArrayList<StockValue> values = new ArrayList<StockValue>()
		StockValue val

		int lineCount = 0

		try{
			url.eachLine(){
				val = importLine(it,lineCount++,relevant)
				if (val != null) values << val
			}
		}catch(FileNotFoundException ex) {
			String errorMessage = "FILENOTFOUND: "+ex
			println errorMessage
			problems << new OutputInfo(stock,MONTH,errorMessage)
			return
		}
		catch(java.net.ConnectException ex){
			String errorMessage = "ConnectException: "+ex
			println errorMessage
			problems << new OutputInfo(stock,MONTH,errorMessage)
			return
		}
		values = values.reverse()

		for(int i = 0;  i < values.size; i++){
			values.get(i).index = i
		}

		return values
	}

	void check(Security stock, int days) {

		String symbol = stock.symbol
		String comdNotationId = stock.comdNotationId
		def lineCount = 0
		def url = createUrl(symbol, days).toURL()
		
		println "New Security: $symbol"

		ArrayList<StockValue> values = importStockValues(stock, url)

		StockValue val

		if (values == null)
			return
		println "\n\n\nAnzahl Werte: $values.size\n\n\n"

		double currentPrice = getCurrentPrice(comdNotationId)
		println "aktueller Preis: $currentPrice"

		StockValue minValue = findMin(values)
		println "Kleinster Wert Datum: $minValue.date Wert:$minValue.relevant"
		
		StockValue maxValue = findMax(values)
		println "Gr��ter Wert Datum: $maxValue.date Wert:$maxValue.relevant"
		
		double maxClose = findMaxClose(values)
		println "Maximum: $maxClose"

		double inc = (maxClose - minValue.relevant) / INCREMENTS
		println "inc=$inc"

		Line lastTrend
		StockValue lastMinValue
		StockValue lastMaxValue

		Line trend
		
		boolean goesDown = isDownTrend(values) 

		if (goesDown)
		{
			println "DOWNTREND"
			trend = findDownTrendLine(values, maxValue.index, inc)
			// hat der Ber�hrpuntk genug Abstand zum Tiefpunkt? (touchIndex-minValue < Significant_distance?
			// Liegt der touchIndex fr�h genut (nicht in den letzten <Ignore_last_days> Tagen?
			while (trend != null && trend.touchIndex-maxValue.index < SIGNIFICANT_DISTANCE && trend.touchIndex < (values.size()-IGNORE_LAST_DAYS) ){
				lastTrend = trend
				lastMaxValue = maxValue
				maxValue = values.get(trend.touchIndex)
				trend = findUpTrendLine(values, maxValue.index, inc)
			}
	
			if (trend == null && lastTrend == null){
				println "COULD NOT FIND ANY TREND"
				noTrends << new OutputInfo(stock,MONTH,currentPrice)
				return
			}
	
			if (trend == null && lastTrend != null){
				println "COULD NOT FIND LONG TREND, TAKING LAST TREND"
				minValue = lastMinValue
				trend = lastTrend
			}
		}
		else
		{
			println "UPTREND"
			trend = findUpTrendLine(values, minValue.index, inc)
			while (trend != null && trend.touchIndex-minValue.index < SIGNIFICANT_DISTANCE && trend.touchIndex < (values.size()-IGNORE_LAST_DAYS) ){
				lastTrend = trend
				lastMinValue = minValue
				minValue = values.get(trend.touchIndex)
				trend = findUpTrendLine(values, minValue.index, inc)
			}
	
			if (trend == null && lastTrend == null){
				println "COULD NOT FIND ANY TREND"
				noTrends << new OutputInfo(stock,MONTH,currentPrice)
				return
			}
	
			if (trend == null && lastTrend != null){
				println "COULD NOT FIND LONG TREND, TAKING LAST TREND"
				minValue = lastMinValue
				trend = lastTrend
			}
		}

		StockValue trendEndValue = values.get(trend.touchIndex)

		if (goesDown)
		{
			println "-----------TREND--------------"
			println "von: $maxValue.date $maxValue.relevant"
			println "bis: $trendEndValue.date $trendEndValue.relevant"
			println "-----------TREND--------------"
		}
		else{
			println "-----------TREND--------------"
			println "von: $minValue.date $minValue.relevant"
			println "bis: $trendEndValue.date $trendEndValue.relevant"
			println "-----------TREND--------------"
		}

		double lastTrendValue = getLastTrendValue(trend,values.size)
		String lastTrendValueFormatted = sprintf("%.2f", lastTrendValue)

		String start = formattedDate(values.get(trend.startX).date);
		double trendDiff = trendDiff(lastTrendValue, currentPrice)
		
		if (goesDown)
		{
			if (isNear(trendDiff) ) {
				downTrendNears << new OutputInfo(stock,MONTH,start,lastTrendValueFormatted,trendDiff,currentPrice,values)
			}
			else {
				downTrendNotNears << new OutputInfo(stock,MONTH,start,lastTrendValueFormatted,trendDiff,currentPrice,values)
			}
		}
		else
		{
			if (isNear(trendDiff) ) {
				upTrendNears << new OutputInfo(stock,MONTH,start,lastTrendValueFormatted,trendDiff,currentPrice,values)
			}
			else {
				upTrendNotNears << new OutputInfo(stock,MONTH,start,lastTrendValueFormatted,trendDiff,currentPrice,values)
			}
		}
		
	}
	
	String formattedDate(Date date)
	{   SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN);
		return dateFormat.format(date); 
	}
	
	String formattedDateTime(Date date)
	{   
		SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMAN);
		return dateTimeFormat.format(date); 
	}
	
	String getTableStart(String tableName)
	{
		return "<table id=\""+tableName+"\" class=\"tablesorter\">"+
		"<thead>"+
		"<tr>"+
			"<th>Trendverhalten</th>"+
			"<th>Name</th>"+
			"<th>Current Price</th>"+
			"<th>Trendstart</th>"+
			"<th>Letzter Wert</th>"+
			"<th>Candle Info</th>"+
			"<th>Trendabstand</th>"+
			"<th><div title=\"6 Month Performance\">6 Month P</div></th>"+
			"<th>4 Week P</th>"+
			"<th>4 Week Var</th>"+
			"<th>P/Var</th>"+
			"<th>DVar�</th>"+
		"</tr>"+
		"</thead>"+
		"<tbody>"
	}
	
	String getTableEnd()
	{
		return "</tbody>"+
		"</table>"
	}
	
	void writeOutput(File outputFile){
		//outputFile.append("<br/>Downtrend trendnah: $downTrendNears.size<br/>")
		Collections.sort(downTrendNears)
		outputFile.append(getTableStart("trendInfos"))
		for (OutputInfo nearOutputInfo : downTrendNears) {
			outputFile.append(nearOutputInfo.getOutputLinesWithTrends("1. Downtrend nahe"))
		}
		//outputFile.append("<br/>DownTrend trendfern: $downTrendNotNears.size<br/>")
		Collections.sort(downTrendNotNears)
		for (OutputInfo notNearOutputInfo : downTrendNotNears) {
			outputFile.append(notNearOutputInfo.getOutputLinesWithTrends("4. Downtrend fern"))
		}
		
		//outputFile.append("<br/>Uptrend trendnah: $upTrendNears.size<br/>")
		Collections.sort(upTrendNears)
		for (OutputInfo nearOutputInfo : upTrendNears) {
			outputFile.append(nearOutputInfo.getOutputLinesWithTrends("2. Uptrend nahe"))
		}
		//outputFile.append("<br/>UpTrend trendfern: $upTrendNotNears.size<br/>")
		Collections.sort(upTrendNotNears)
		for (OutputInfo notNearOutputInfo : upTrendNotNears) {
			outputFile.append(notNearOutputInfo.getOutputLinesWithTrends("3. Uptrend fern"))
		}
		outputFile.append(getTableEnd())

		outputFile.append("<br/>ohne Trend: $noTrends.size<br/>")
		for (OutputInfo noTrendOutputInfo : noTrends) {
			outputFile.append(noTrendOutputInfo.getOutputLinesWithoutTrends())
		}
		outputFile.append("<br/>Probleme: $problems.size<br/>")
		for (OutputInfo problemOutputInfo : problems) {
			outputFile.append(problemOutputInfo.getOutputLinesForProblems())
		}
	}

    static String formatNumber(double f)
    {
        return String.format(Locale.GERMAN, "%.2f", f)
    }


	public static void main(String[] args){
		DepotCheck depotCheck = new DepotCheck()

        if (args.length == 1 && args[0].equals("help")){
			println "groovy DepotCheck.groovy"
			println "erzeugt depotcheck.html"
			return
		}

		println "Program started"

		ArrayList<Security> securities = new ArrayList<Security>()
		File depositFile = new File(".\\src\\depot.csv")
		depotCheck.importStocks(securities, depositFile)

        depotCheck.importHistoricalData securities

        securities.each {
			//println it.wkn+" "+it.name+ "Kurs: "+ depotCheck.fetchCurrentPrice(it.comdNotationId+"")
		}

        securities.each{
            println it.wkn+" "+it.name+" 2012-11-27: "+ it.notationFrom(LocalDate.parse("2012-11-27",DepotCheck.sortFormat))
        }

        File outputFile = new File('.\\depotcheck.csv')
        outputFile.write("Name"+DEL+"wkn"+DEL+"Depot"+DEL+"buyDate"+DEL+"Kaufkurs"+DEL+"Kurs heute"+DEL+"Perf 3Y"+DEL+"Perf 3M"+DEL+"Perf 1M"+DEL+"Perf since buy"+DEL+"abs. Diff"+DEL+"Wert")
        outputFile.append("\n");

        LocalDate now = LocalDate.now()


        securities.each {
            println "fetch Current:"
            double todaysPrice = depotCheck.fetchCurrentPrice(it.comdNotationId+"")
            println "todaysPrice "+todaysPrice
            println "fetch 3Y:"
            Double threeYearsAgo = it.notationFrom(now.minusYears(3))
            if (threeYearsAgo == 0.0d)
                threeYearsAgo = 0.1d
            println "threeYearsAgo "+threeYearsAgo
            double ThreeYPerf = ((todaysPrice / threeYearsAgo)-1) * 100
            println "fetch 3M"
            Double threeMonthAgo = it.notationFrom(now.minusMonths(3))
            if (threeMonthAgo == 0.0d)
                threeMonthAgo = 0.1d
            println "threeMonthAgo "+threeMonthAgo
            double ThreeMPerf = ((todaysPrice / threeMonthAgo)-1)*100
            println "3M %:"+ThreeMPerf
            println "fetch 1M"
            //String tmp = printf(Locale.GERMAN, "%-10.2f", ThreeMPerf)
            Double oneMonthAgo = it.notationFrom(now.minusMonths(1))
            if (oneMonthAgo == 0)
                oneMonthAgo = 0.1d
            println "oneMonthAgo" + oneMonthAgo
            double OneMPerf = ((todaysPrice / oneMonthAgo)-1)*100
			println "fetch since buy"
			double perfSinceBuy = ((todaysPrice / it.buyPrice)-1)*100

			double absDiffSinceBuy = (todaysPrice * it.amount)-(it.buyPrice * it.amount)
			double absValue = (todaysPrice*it.amount)
            println "output"
            outputFile.append(
					it.name+DEL+"'"+
							it.wkn+"'"+DEL+
							it.deposit+DEL+
							it.buyDate+DEL+
							formatNumber(it.buyPrice)+DEL+
							formatNumber(todaysPrice)+DEL+
							formatNumber(ThreeYPerf)+DEL+
							formatNumber(ThreeMPerf)+DEL+
							formatNumber(OneMPerf)+DEL+
							formatNumber(perfSinceBuy)+DEL+
							formatNumber(absDiffSinceBuy)+DEL+
							formatNumber(absValue))
            outputFile.append("\n")
        }

        println "program finished"

		return;

		//File outputFile = new File('.\\depotcheck.html')
		String today = depotCheck.formattedDateTime(new Date())

		for (int i = 0; i < securities.size(); i++) {
			depotCheck.check(securities.get(i),20)
		}
		outputFile.write "<html><head>"
		outputFile.append "<title>Trendcheck</title>"
		outputFile.append "<link rel=\"icon\" href=\"favicon.ico\" type=\"image/x-icon\">"
		outputFile.append "<link href=\"../../css/theme.blue.css\" rel=\"stylesheet\">"
		outputFile.append "<script type=\"text/javascript\" src=\"../../lib/jquery-2.0.3.min.js\"></script>"
		outputFile.append "<script type=\"text/javascript\" src=\"../../lib/jquery.tablesorter.min.js\"></script>"
		outputFile.append "<script type=\"text/javascript\" src=\"../../lib/jquery.tablesorter.widgets.min.js\"></script>"
		outputFile.append "<script type=\"text/javascript\">"
		outputFile.append "\$(document).ready(function() {"
		outputFile.append "\$(\"#trendInfos\").tablesorter( {theme : 'blue', sortList: [[0,0], [6,0]],widgets:['zebra']} );}) "
		outputFile.append "</script>"
		outputFile.append "</head><body>"
		if (depotCheck.relevant == StockValue.CLOSE)
			outputFile.append("<p><b>Schlusskurse $depotCheck.MONTH Monate $today</b></p>\n")
		else
			outputFile.append("<p><b>Tiefstkurse $depotCheck.MONTH Monate $today</b></p>\n")
		depotCheck.writeOutput(outputFile)
		outputFile.append "</body></html>"
		println "fertig mit $securities.size Werten"
	}
}
