package com.hongminh54.phoban.utils;

import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;

public class Random {
	
	private java.util.List<Chance> chances;
	private double sum;
	private java.util.Random random;
	  
	private class Chance {
		
		private double upperLimit;
	    private double lowerLimit;
	    private Object element;
	    
	    public Chance(Object element, double lowerLimit, double upperLimit) {
	    	this.element = element;
	    	this.upperLimit = upperLimit;
	    	this.lowerLimit = lowerLimit;
	    }
	    
	    public double getUpperLimit() {
	    	return upperLimit;
	    }
	    
	    public double getLowerLimit() {
	    	return lowerLimit;
	    }
	    
	    public Object getElement() {
	    	return element;
	    }
	    
	    public String toString() {
	    	return "[" + Double.toString(lowerLimit) + "|" + Double.toString(upperLimit) + "]: " + element.toString();
	    }
	}
	  




	  public Random() {
		  random = new java.util.Random();
		  chances = new java.util.ArrayList();
		  sum = 0;
	  }
	  
	  public Random(long seed) {
		  random = new java.util.Random(seed);
		  chances = new java.util.ArrayList();
		  sum = 0;
	  }
	  
	  public void addChance(Object element, double chance) {
		  // Kiểm tra xem phần tử đã tồn tại chưa
		  boolean exists = false;
		  for(Chance c : chances) {
			  if(c.getElement().equals(element)) {
				  exists = true;
				  break;
			  }
		  }
		  
		  if(!exists) {
			  chances.add(new Chance(element, sum, sum + chance));
			  sum += chance;
		  }
	  }

	  public void removeChance(Object element) {
		Iterator<Chance> iter = chances.iterator();
		boolean removeSum = false;
		double sum = 0d;
		while(iter.hasNext()) {
			Chance chance = iter.next();
			if(removeSum) {
				chance.lowerLimit -= sum;
				chance.upperLimit -= sum;
			}
			if(chance.element.equals(element)) {
				iter.remove();
				removeSum = true;
				sum = chance.upperLimit - chance.lowerLimit;
				this.sum -= sum;
			}
		}
	  }

	  public Object getRandomElement() {
		  if (chances.isEmpty() || sum <= 0) {
			  return null;
		  }
		  
		  double index = ThreadLocalRandom.current().nextDouble(0, sum);
		  
			  for(Chance chance : chances) {
				  if((chance.getLowerLimit() <= index) && (chance.getUpperLimit() > index)) {
				  return chance.getElement();
				  }
			  }
		  
			  index = ThreadLocalRandom.current().nextDouble(0, sum);
		  for(Chance chance : chances) {
			  if((chance.getLowerLimit() <= index) && (chance.getUpperLimit() > index)) {
				  return chance.getElement();
			  }
		  }
		  
		  return chances.isEmpty() ? null : chances.get(0).getElement();
	  }
	  
	  public double getOptions() {
		  return sum;
	  }
	  
	  public int getChoices() {
		  return chances.size();
	  }

}
