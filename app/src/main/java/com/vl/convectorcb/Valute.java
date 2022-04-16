package com.vl.convectorcb;

public class Valute {
    final static String RUB_ID = "RUBLE";

    public static Valute getRuble() {
        Valute ruble = new Valute();
        ruble.ID = RUB_ID;
        ruble.CharCode = "RUB";
        ruble.Name = "Рубль";
        ruble.Nominal = 1;
        ruble.Value = 1;
        return ruble;
    }

    private String ID, NumCode, CharCode, Name;
    private double Previous, Value;
    private int Nominal;

    public String getID() {
        return ID;
    }

    public String getNumCode() {
        return NumCode;
    }

    public String getCharCode() {
        return CharCode;
    }

    public String getName() {
        return Name;
    }

    public double getPrevious() {
        return Previous;
    }

    public double getValue() {
        return Value;
    }

    public int getNominal() {
        return Nominal;
    }
}
