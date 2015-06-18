import java.time.LocalDate

class Security {
	String wkn;
	String deposit;
	String name;
    Float buyPrice;
	String buyDate;
	HashMap<String, Float> historicalData;
    String firstDate;
	
	int comdNotationId;

    Security(String[] value) {
		assert value.length == 6
		this.wkn = value[0]
		this.deposit = value[1].trim()
		this.name = value[2].trim()
		this.buyDate = value[3].trim()
        this.buyPrice = Float.parseFloat(value[4].trim().replaceAll(",","."))
		this.comdNotationId = Integer.parseInt(value[5].trim())
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


}
