package arcgissamples.soe;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.esri.arcgis.carto.IMapLayerInfo;
import com.esri.arcgis.carto.IMapLayerInfos;
import com.esri.arcgis.carto.IMapServer3;
import com.esri.arcgis.carto.IMapServerDataAccess;
import com.esri.arcgis.carto.IMapServerInfo;
import com.esri.arcgis.geodatabase.FeatureClass;
import com.esri.arcgis.interop.AutomationException;
import com.esri.arcgis.interop.extn.ArcGISExtension;
import com.esri.arcgis.interop.extn.ServerObjectExtProperties;
import com.esri.arcgis.server.IServerObjectExtension;
import com.esri.arcgis.server.IServerObjectHelper;
import com.esri.arcgis.server.json.JSONArray;
import com.esri.arcgis.server.json.JSONException;
import com.esri.arcgis.server.json.JSONObject;
import com.esri.arcgis.system.ILog;
import com.esri.arcgis.system.IRESTRequestHandler;
import com.esri.arcgis.system.ServerUtilities;

// import json.topojson.api.TopojsonApi;

@ArcGISExtension@ServerObjectExtProperties(displayName = "Topojson", description = "Provides GeoData as Topojson", defaultSOAPCapabilities = "", allSOAPCapabilities = "", properties = "")
public class TopojsonSOE implements IServerObjectExtension,
IRESTRequestHandler {
	private static final long serialVersionUID = 1L;
	private IServerObjectHelper soHelper;
	private ILog serverLog;
	private IMapLayerInfos layerInfos;

	public TopojsonSOE() throws Exception {
		super();
	}

	/****************************************************************************************************************************
	 * IServerObjectExtension methods: This is a mandatory interface that must
	 * be supported by all SOEs. This interface is used by the Server Object to
	 * manage the lifetime of the SOE and includes two methods: init() and
	 * shutdown(). The Server Object cocreates the SOE and calls the init()
	 * method handing it a back reference to the Server Object via the Server
	 * Object Helper argument. The Server Object Helper implements a weak
	 * reference on the Server Object. The extension can keep a strong reference
	 * on the Server Object Helper (for example, in a member variable) but
	 * should not keep a strong reference on the Server Object. The log entries
	 * are merely informative and completely optional.
	 ****************************************************************************************************************************/
	/**
	 * init() is called once, when the instance of the SOE is created.
	 */
	public void init(IServerObjectHelper soh) throws IOException,
	AutomationException {
		/*
		 * An SOE should get the Server Object from the Server Object Helper in
		 * order to make any method calls on the Server Object and release the
		 * reference after making the method calls.
		 */
		this.soHelper = soh;
		this.serverLog = ServerUtilities.getServerLogger();

		serverLog.addMessage(3, 200, "Beginning init in " + this.getClass().getName() + " SOE.");

		IMapServer3 ms = (IMapServer3) this.soHelper.getServerObject();
		IMapServerInfo mapServerInfo = ms.getServerInfo(ms.getDefaultMapName());
		this.layerInfos = mapServerInfo.getMapLayerInfos();

		serverLog.addMessage(3, 200, "Initialized " + this.getClass().getName() + " SOE.");
	}

	/**
	 * shutdown() is called once when the Server Object's context is being shut
	 * down and is about to go away.
	 */
	public void shutdown() throws IOException, AutomationException {
		/*
		 * The SOE should release its reference on the Server Object Helper.
		 */
		serverLog.addMessage(3, 200, "Shutting down " + this.getClass().getName() + " SOE.");
		this.soHelper = null;
		this.serverLog = null;
	}

	/**
	 * Returns number of layers in associate map service, based on layer type
	 * provided
	 * 
	 * @param operationInput
	 * @return
	 */
	private byte[] getLayerCountByType(JSONObject operationInput, java.util.Map < String, String > responsePropertiesMap)
	throws Exception {
		String type = operationInput.getString("type");

		JSONObject json = new JSONObject();

		int count = 0;
		if (type != null && !type.isEmpty()) {
			String aoType = "";
			if (type.equalsIgnoreCase("all")) {
				count = layerInfos.getCount();
			} else if (type.equalsIgnoreCase("feature")) {
				aoType = "Feature Layer";
			} else if (type.equalsIgnoreCase("raster")) {
				aoType = "Raster Layer";
			} else if (type.equalsIgnoreCase("dataset")) {
				aoType = "Network Dataset Layer";
			}

			for (int i = 0; i < layerInfos.getCount(); i++) {
				if (layerInfos.getElement(i).getType().equalsIgnoreCase(aoType)) {
					count++;
				}
			}

			json.put("count", count);

			responsePropertiesMap.put("Content-Type", "application/json");

			return json.toString().getBytes();
		} else {
			throw new Exception(
				"Invalid layer type provided. Available types are: \"all\", \"feature\", \"raster\", \"dataset\".");
		}
	}

	/**
	 * Returns JSON representation of root resource's description.
	 * 
	 * @return String JSON representation of root resource's description.
	 */
	private byte[] getRootResource() throws Exception {
		JSONObject json = new JSONObject();
		json.put("name", "Java Simple REST SOE");
		json.put(
			"description",
			"Simple REST SOE with 1 sub-resource called \"layers\" and 1 operation called \"getLayerCountByType\".");
		json.put(
			"usage",
			"The \"layers\" subresource returns all layers in the map service.\n" + "The \"getLayerCountByType\" operation returns a count of layer of specified type. It accepts one of the following values as input: \"feature\", \"raster\", " + "\"dataset\", and \"all\".");
		return json.toString().getBytes("utf-8");
	}

	/**
	 * Returns JSON representation of specified sub-resource.
	 * 
	 * @return String JSON representation of specified resource.
	 */
	private byte[] getSubresourceLayers(Map < String, String > responsePropertiesMap) throws Exception {
		JSONObject json = new JSONObject();
		json.put("layers", getLayersInfoAsJSON());

		responsePropertiesMap.put("Content-Type", "application/json");

		return json.toString().getBytes("utf-8");
	}

	/**
	 * Returns info about layers in associated map service
	 * 
	 * @return
	 * @throws Exception
	 */
	public JSONObject getLayersInfoAsJSON() throws Exception {
		JSONObject json = new JSONObject();

		int count = this.layerInfos.getCount();
		json.put("layerCount", count);
		JSONArray layerArray = new JSONArray();
		for (int i = 0; i < count; i++) {
			IMapLayerInfo layerInfo = layerInfos.getElement(i);
			JSONObject layerJSON = new JSONObject();
			layerJSON.put("name", layerInfo.getName());
			String layerType = layerInfo.getType();
			layerJSON.put("type", layerType);
			int id = layerInfo.getID();
			layerJSON.put("id", id);
			layerJSON.put("description", layerInfo.getDescription());
			if (layerInfo.isFeatureLayer()) {
				IMapServerDataAccess mapServerDataAccess = (IMapServerDataAccess) this.soHelper.getServerObject();
				FeatureClass fc = new FeatureClass(
				mapServerDataAccess.getDataSource("", id));
				layerJSON.put("featureCount", fc.featureCount(null));
			}

			layerArray.put(i, layerJSON);
		}
		json.put("layersInfo", layerArray);
		return json;
	}

	/**
	 * Returns JSON representation of specified resource.
	 * 
	 * @return String JSON representation of specified resource.
	 */
	private byte[] getResource(String resourceName, Map < String, String > responsePropertiesMap) throws Exception {
		if (resourceName.equalsIgnoreCase("") || resourceName.length() == 0) {
			return getRootResource();
		} else if (resourceName.equalsIgnoreCase("layers")) {
			return getSubresourceLayers(responsePropertiesMap);
		}

		return null;
	}

	/**
	 * Invokes specified REST operation on specified REST resource
	 * 
	 * @param capabilitiesList
	 * @param resourceName
	 * @param operationName
	 * @param operationInput
	 * @return
	 */
	private byte[] invokeRESTOperation(String capabilitiesList,
	String resourceName, String operationName, String operationInput,
	String outputFormat, String requestProperties,
	Map < String, String > responsePropertiesMap) throws Exception {
		byte[] operationOutput = null;

		JSONObject operationInputAsJSON = new JSONObject(operationInput);

		if (resourceName.isEmpty()) {
			if (operationName.equalsIgnoreCase("getLayerCountByType")) {
				operationOutput = getLayerCountByType(operationInputAsJSON, responsePropertiesMap);
			}
		} else
		// if non existent sub-resource specified, report error
		{
			this.serverLog.addMessage(1, 500, "No sub-resource by name " + resourceName + " found.");
			return ServerUtilities.sendError(500,
				"No sub-resource by name " + resourceName + " found.",
			new String[] {
				"No details specified."
			})
				.getBytes("utf-8");
		}

		return operationOutput;
	}

	/*************************************************************************************
	 * IRESTRequestHandler methods:
	 *************************************************************************************/
	/**
	 * This method handles REST requests by determining whether an operation or
	 * resource has been invoked and then forwards the request to appropriate
	 * methods.
	 */
	public byte[] handleRESTRequest(String capabilities, String resourceName,
	String operationName, String operationInput, String outputFormat,
	String requestProperties, String[] responseProperties)
	throws IOException, AutomationException {
		/*
		 * This method handles REST requests by determining whether an operation
		 * or resource has been invoked and then forwards the request to
		 * appropriate methods.
		 */
		try {
			Map < String, String > responsePropertiesMap = new HashMap < String, String > ();


			// if no operationName is specified send description of specified
			// resource
			byte[] response = null;
			if (operationName.length() == 0) {
				response = getResource(resourceName, responsePropertiesMap);
			} else
			// invoke REST operation on specified resource
			{
				response = invokeRESTOperation(capabilities, resourceName,
				operationName, operationInput, outputFormat,
				requestProperties, responsePropertiesMap);
			}
			

			//collect response properties that may have changed in subresources or operations
			JSONObject responsePropertiesJSON = new JSONObject(responsePropertiesMap);
			responseProperties[0] = responsePropertiesJSON.toString();

			return response;
		} catch (Exception e) {
			this.serverLog.addMessage(1, 500, e.getMessage());
			return ServerUtilities.sendError(500,
				"Exception occurred: " + e.getMessage(),
			new String[] {
				"No details specified."
			}).getBytes("utf-8");
		}
	}

	/**
	 * This method returns the resource hierarchy of a REST based SOE in JSON
	 * format.
	 */
	public String getSchema() throws IOException, AutomationException {
		try {
			JSONObject simpleRESTSOE = ServerUtilities.createResource(
				"JavaSimpleRESTSOE", "Java Simple REST SOE", false, false);
			JSONArray _subResourcesArray = new JSONArray();
			_subResourcesArray.put(ServerUtilities.createResource("layers",
				"layers in associated map service", false, false));
			simpleRESTSOE.put("resources", _subResourcesArray);

			JSONArray _OpArray = new JSONArray();
			_OpArray.put(ServerUtilities.createOperation("getLayerCountByType",
				"type", "json", false));
			simpleRESTSOE.put("operations", _OpArray);
			return simpleRESTSOE.toString();
		} catch (JSONException e) {
			this.serverLog.addMessage(1, 500, e.getMessage());
			return ServerUtilities.sendError(500,
				"Exception occurred: " + e.getMessage(), null);
		}
	}
}