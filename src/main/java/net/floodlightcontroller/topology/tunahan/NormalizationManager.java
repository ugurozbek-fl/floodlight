package net.floodlightcontroller.topology.tunahan;

import net.floodlightcontroller.linkdiscovery.Link;

import java.util.Map;

public class NormalizationManager {
    public static final int StandardScoreShifting =2;

    public static long getMaxBandWidth(Map<Link, Integer> linkBandWith) {
        long tempVal = 0L;
        try {
            for (Link link : linkBandWith.keySet()) {
                if (link != null && linkBandWith.get(link) > tempVal) {
                    tempVal = linkBandWith.get(link);
                }

            }
        } catch (Exception ex) {
            tempVal = 0;
        }
        return tempVal + 20;
    }

    public static long getMinBandWidth(Map<Link, Integer> linkBandWith) {
        long tempVal = Long.valueOf(9999999);
        try {
            for (Link link : linkBandWith.keySet()) {
                if (link != null && linkBandWith.get(link) < tempVal) {
                    tempVal = linkBandWith.get(link);
                }
            }
        } catch (Exception ex) {
            tempVal = 0;
        }
        return tempVal > 50 ? tempVal - 20 : tempVal;
    }

    public static double getNormalizedBandWidth(Map<Link, Integer> linkBandWith, Link link) {
        //returns katsayı
        double x = 1;
        long BandWidth = linkBandWith.get(link);
        long minBandWidth = NormalizationManager.getMinBandWidth(linkBandWith);
        long maxBandWidth = NormalizationManager.getMaxBandWidth(linkBandWith);
        if (maxBandWidth == minBandWidth) {
            return x;
        } else {
            x = ((double) (BandWidth - minBandWidth)) / ((double) (maxBandWidth - minBandWidth));
        }
        return x;
    }

    //2 Ocak günü tavsiye üzerine eklendi. aslinda basitce getCost yerini almasi icin yazildi
    //bunun disinde 1/x mantigi olan kod olmamalı bu sınıf içinde
    public static double getMixedCost(Link link, Map<Link, Integer> cost, Map<Link, Integer> linkBandWith) {
        double normalizedMinBandWidth = NormalizationManager.getNormalizedBandWidth(linkBandWith, link);
        return (((double) cost.get(link)) / normalizedMinBandWidth); // 1/x muhabbeti
    }

    //2 Ocak günü tavsiye üzerine eklendi. aslinda basitce getCost yerini almasi icin yazildi
    //bunun disinde 1/x mantigi olan kod olmamalı bu sınıf içinde
    //
    public static double getMixedCost2(Link link, Map<Link, Integer> cost, Map<Link, Integer> linkBandWith) {
        double normalizedMinBandWidth = NormalizationManager.StandardScoreShifting + NormalizationManager.getNormalizedBandWidth2(linkBandWith, link);
        return (((double) cost.get(link)) / normalizedMinBandWidth); // 1/x muhabbeti
    }


    private static double standardDeviation(Map<Link, Integer> linkBandWith) {
        double total = 0;
        double middleValue = arithmeticMean(linkBandWith);
        for (Integer bw : linkBandWith.values()) {
            total += Math.pow((bw - middleValue), 2);
        }

        if (linkBandWith != null && linkBandWith.size() > 0 && total > 0) {
            total /= linkBandWith.size();
            total = Math.sqrt(total);
        }
        if (total == 0.0) total = 1.0; // can not be 0
        return total;


    }

    private static double arithmeticMean(Map<Link, Integer> linkBandWith) {
        double middleValue = 0;
        for (Integer bw : linkBandWith.values()) {
            middleValue = middleValue + bw;
        }
        middleValue = middleValue / linkBandWith.size();
        return middleValue;
    }

    public static double getNormalizedBandWidth2(Map<Link, Integer> linkBandWith, Link link) {
        //returns only katsyı
        double standardDeviation = standardDeviation(linkBandWith);
        double arithmeticMean = arithmeticMean(linkBandWith);
        double linkBw = linkBandWith.get(link);
        double result = (linkBw - arithmeticMean) / standardDeviation;
        return result;
    }

    //probability
    public static boolean isLinkUnavailable(double p) {
        if (p < 0) p = 0;
        else if (p > 1) p = 1;
        return Math.random() < p;
    }
}
