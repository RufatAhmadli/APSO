package CustomerData;

public class ServiceResult {
    private final int customerId;      // links back to CustomerData.CustomerRecord.id
    private final double arrival;      // copied in; lets us compute waitTime here

    private double serviceStart = -1;  // -1 = "not set yet" (bug sentinel)
    private double serviceEnd   = -1;
    private boolean abandoned = false;

    public ServiceResult(int customerId, double arrival) {
        this.customerId = customerId;
        this.arrival = arrival;
    }

    public int getCustomerId() {
        return customerId;
    }

    public double getArrival() {
        return arrival;
    }

    public double getServiceStart() {
        return serviceStart;
    }

    public double getServiceEnd() {
        return serviceEnd;
    }

    public boolean isAbandoned() {
        return abandoned;
    }

    public void setAbandoned(boolean abandoned) {
        this.abandoned = abandoned;
    }

    public void setServiceStart(double serviceStart) {
        this.serviceStart = serviceStart;
    }

    public void setServiceEnd(double serviceEnd) {
        this.serviceEnd = serviceEnd;
    }

    public double getWaitTime() {
        return serviceStart - arrival;
    }

    public double getExecutionTime() {
        return serviceEnd - serviceStart;
    }
}
