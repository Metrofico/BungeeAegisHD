package xyz.yooniks.aegis.vpn;

import java.io.IOException;

public final class AddressInfoResponse {

  private String country, countryCode, status, city, region, regionName, org, as;
  private boolean proxy;

  public AddressInfoResponse() {
  }

  AddressInfoResponse(boolean proxy) {
    this.proxy = proxy;
  }

  public String getCountry() {
    return country;
  }

  public String getCountryCode() {
    return countryCode;
  }

  public String getStatus() {
    return status;
  }

  public String getCity() {
    return city;
  }

  public String getRegion() {
    return region;
  }

  public String getRegionName() {
    return regionName;
  }

  public String getOrg() {
    return org;
  }

  public boolean isProxy() {
    return proxy;
  }

  public String getAs() {
    return as;
  }

  @Override
  public String toString() {
    return "AddressInfoResponse{" +
        "country='" + country + '\'' +
        ", countryCode='" + countryCode + '\'' +
        ", status='" + status + '\'' +
        ", city='" + city + '\'' +
        ", region='" + region + '\'' +
        ", regionName='" + regionName + '\'' +
        ", org='" + org + '\'' +
        ", as='" + as + '\'' +
        ", proxy=" + proxy +
        '}';
  }

  public interface VPNResponsable {

    AddressInfoResponse info(String address) throws IOException;
  }

}
