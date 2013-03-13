import java.io.Serializable;

public class Message implements Serializable
{
  private int id;
  private int correlationId;
  private String payload;

  public int getId()
  {
    return id;
  }
  public void setId(int id)
  {
    this.id = id;
  }
  public int getCorrelationId()
  {
    return correlationId;
  }
  public void setCorrelationId(int correlationId)
  {
    this.correlationId = correlationId;
  }
  public String getPayload()
  {
    return payload;
  }
  public void setPayload(String payload)
  {
    this.payload = payload;
  }
  
}
