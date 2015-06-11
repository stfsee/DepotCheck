class Security {
	String wkn;
	String deposit;
	String name;
    Float buyPrice;
	String buyDate;
	
	int comdNotationId;

    Security(String[] value) {
		assert value.length == 6
		this.wkn = value[0]
		this.deposit = value[1].trim()
		this.name = value[2].trim()
		this.buyDate = value[3].trim()
        this.buyPrice = Float.parseFloat(value[4].trim().replaceAll(",","."))
		this.comdNotationId = Integer.parseInt(value[5].trim())
	}
}
