package com.anya.printio;

public class PrintServiceException extends Exception {
  private static final long serialVersionUID = 5970295698620582665L;

  public PrintServiceException(String msg) {
    super(msg);
  }
  
  public PrintServiceException(Throwable t) {
    super(t);
  }
  
  public PrintServiceException(String msg, Throwable t) {
    super(msg, t);
  }
}
