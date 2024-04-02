package services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import models.Security;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import xml.XmlHelper;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static constants.PathConstants.CACHE_PATH;

public class SecurityService {
    private static final Logger logger = Logger.getLogger(SecurityService.class.getCanonicalName());
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");
    XmlHelper xmlHelper = new XmlHelper();
    private String cachePath = CACHE_PATH;

    public SecurityService() {
    }

    public SecurityService(String cachePath) {
        this.cachePath = cachePath;
    }

    public List<Security> processSecurities(NodeList oAllSecurities) {
        List<Security> securities = new ArrayList<>();
        for (int i = 0; i < oAllSecurities.getLength(); i++) {
            Security security = processSecurity((Element) oAllSecurities.item(i));
            securities.add(security);
        }

        return securities;
    }

    Security processSecurity(Element securitiesElement) {
        Security security = null;
        String isin = xmlHelper.getTextContent(securitiesElement, "isin");
        String isRetired = xmlHelper.getTextContent(securitiesElement, "isRetired");

        if (!isin.isEmpty() && "false".equals(isRetired)) {
            security = createSecurity(isin);
            String name = xmlHelper.getTextContent(securitiesElement, "name");
            if ((security.getName() == null || security.getName().isEmpty()) && !name.isEmpty()) {
                security.setName(name);
            }
        }

        return security;
    }

    Security createSecurity(String strIsin) {
        Security security = new Security(strIsin);
        try {
            SecurityDetails securityDetails = new SecurityDetails(cachePath, strIsin);

            boolean isEftOrFond = securityDetails.isETF() || securityDetails.isFond();
            logger.fine(" - is ETF or Fond: " + isEftOrFond);
            security.setFond(isEftOrFond);
            if (isEftOrFond) {
                JsonObject breakdownsNode = securityDetails.getBreakDownForSecurity();

                if (breakdownsNode != null) {
                    // parsing holdings
                    Map<String, Double> oListForHoldings = getHoldingPercentageMap(breakdownsNode);
                    security.setHoldings(oListForHoldings);

                    // parsing branches
                    security.setIndustries(getMappedPercentageForNode(breakdownsNode.getAsJsonObject("branchBreakdown")));

                    // parsing country
                    security.setCountries(getMappedPercentageForNode(breakdownsNode.getAsJsonObject("countryBreakdown")));
                }
            } else {
                String industry = securityDetails.getIndustry();
                Map<String, Double> industriesMap = new HashMap<>();
                industriesMap.put(industry, 100.0);
                security.setIndustries(industriesMap);

                String country = securityDetails.getCountryForSecurity();
                Map<String, Double> countryMap = new HashMap<>();
                countryMap.put(country, 100.0);
                security.setCountries(countryMap);

                String companyName = securityDetails.getName();
                security.setName(companyName);
                Map<String, Double> holdingsMap = new HashMap<>();
                holdingsMap.put(companyName, 100.0);
                security.setHoldings(holdingsMap);

                logger.fine("Setting name \"" + companyName + "\" and industry \"" + industry + "\" and country \"" + country + "\" to security: " + security);
            }
        } catch (Exception e) {
            logger.warning("Error loading details for " + strIsin + " from " + cachePath + ": " + e.getMessage());
        }
        return security;
    }

    Map<String, Double> getMappedPercentageForNode(JsonObject oNode) {
        Map<String, Double> oResultList = new HashMap<>();
        if (oNode != null) {
            JsonArray oArrayList = oNode.getAsJsonArray("list");
            for (int i = 0; i < oArrayList.size(); i++) {
                JsonObject oNodeInsideArray = ((JsonObject) oArrayList.get(i));
                String strName = oNodeInsideArray.get("nameBreakdown").getAsString();
                Double nPercent = oNodeInsideArray.get("investmentPct").getAsDouble();
                String nameFundsBreakdown = oNode.get("nameFundsBreakdown").getAsString();
                logger.finer("nameBreakdown: " + strName + " - nameFundsBreakdown: " + nameFundsBreakdown);
                if (!strName.equals("Barmittel") || nameFundsBreakdown.equals("Instrument")) {
                    oResultList.put(strName, nPercent);
                    logger.fine(String.format("name: %s; Percentage: %s%%", strName, DECIMAL_FORMAT.format(nPercent)));
                }
            }
        }
        return oResultList;
    }

    Map<String, Double> getHoldingPercentageMap(JsonObject breakdownsNode) {
        Map<String, Double> oListForHoldings = new HashMap<>();
        JsonObject fundsHoldingList = breakdownsNode.getAsJsonObject("fundsHoldingList");
        JsonArray holdingListArray = fundsHoldingList != null ? fundsHoldingList.getAsJsonArray("list") : new JsonArray();

        for (int i = 0; i < holdingListArray.size(); i++) {
            JsonObject oHolding = ((JsonObject) holdingListArray.get(i));
            String strHoldingName = oHolding.getAsJsonObject("instrument").get("name").getAsString();
            Double nHoldingPercent = oHolding.get("investmentPct").getAsDouble();
            logger.fine(String.format("Holding: %s; Percentage: %s%%", strHoldingName, DECIMAL_FORMAT.format(nHoldingPercent)));
            oListForHoldings.put(strHoldingName, nHoldingPercent);
        }
        return oListForHoldings;
    }

}
