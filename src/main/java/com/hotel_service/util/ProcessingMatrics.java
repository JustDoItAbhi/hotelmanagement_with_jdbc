package com.hotel_service.util;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProcessingMatrics {
    private long apiResponseTime;
    private long databaseQueryTime;
    private String memoryUsage;
    private String apiName;
    private double errorTime;
    private long numberOfActiveUsers;

    public void print() {
        System.out.println("\n PERFORMANCE METRICS:");
        System.out.println("• API Response Time: " + apiResponseTime + "ms average");
        System.out.println("• Database Query Time: " + databaseQueryTime + "ms average");
        System.out.println("• Memory Usage: " + memoryUsage);
        System.out.println("• API NAME : "+apiName );
        System.out.println("• ERROR RATE : "+errorTime );
        System.out.println("• Concurrent Users:"+numberOfActiveUsers+ "+ supported");
        System.out.println("-----------------------------------");
    }
}
