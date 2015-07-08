import java.time.LocalDate
import java.time.format.DateTimeFormatter

class Security {
	String wkn;
	String deposit;
	String name;
    int amount;
    Float buyPrice;
	String buyDate;
	HashMap<String, Float> historicalData;
    String firstDate;
	
	int comdNotationId;

    Security(String[] value) {
		//assert value.length == 7
		this.wkn = value[0]
		this.deposit = value[1].trim()
		this.name = value[2].trim()
        this.amount = Integer.parseInt(value[3].trim())
		this.buyDate = value[4].trim()
        this.buyPrice = Float.parseFloat(value[5].trim().replaceAll(",","."))
		this.comdNotationId = Integer.parseInt(value[6].trim())
        //2015-12-22, 3.14159
		historicalData = new HashMap<String, Float>();
	}

    float notationFrom(LocalDate then){
        String thenString = DepotCheck.sortFormat.format(then)
        if (thenString < firstDate)
            return 0.0f
        Float returnValue = historicalData.get(thenString)
        while (returnValue==null)
        {
            then = then.minusDays(1)
            thenString = DepotCheck.sortFormat.format(then)
            if (thenString < firstDate)
                return 0.0f
            returnValue = historicalData.get(thenString)
        }
        return returnValue.floatValue()
    }

    float notationForm(String dateString){
        LocalDate date;
        if (dateString.contains("-"))
            date = new LocalDate(dateString);
        if (dateString.contains(".")) {
            final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            date = LocalDate.parse(dateString, dtf)
        }
        return notationFrom(date)
    }


}
