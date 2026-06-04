package com.eveningoutpost.dexdrip.carblookup.model;

public class PortionItemResult {
    private String productName;
    private String barcode;
    private String brand;
    private double carbsPer100g;
    private double scaledWeightGrams;
    private double scaledCarbsGrams;
    private int giEstimate;
    private double scaledGlEstimate;

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public double getCarbsPer100g() {
        return carbsPer100g;
    }

    public void setCarbsPer100g(double carbsPer100g) {
        this.carbsPer100g = carbsPer100g;
    }

    public double getScaledWeightGrams() {
        return scaledWeightGrams;
    }

    public void setScaledWeightGrams(double scaledWeightGrams) {
        this.scaledWeightGrams = scaledWeightGrams;
    }

    public double getScaledCarbsGrams() {
        return scaledCarbsGrams;
    }

    public void setScaledCarbsGrams(double scaledCarbsGrams) {
        this.scaledCarbsGrams = scaledCarbsGrams;
    }

    public int getGiEstimate() {
        return giEstimate;
    }

    public void setGiEstimate(int giEstimate) {
        this.giEstimate = giEstimate;
    }

    public double getScaledGlEstimate() {
        return scaledGlEstimate;
    }

    public void setScaledGlEstimate(double scaledGlEstimate) {
        this.scaledGlEstimate = scaledGlEstimate;
    }
}