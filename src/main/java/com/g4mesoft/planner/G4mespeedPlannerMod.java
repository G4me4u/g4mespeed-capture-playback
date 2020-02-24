package com.g4mesoft.planner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.g4mesoft.core.GSIModuleManager;
import com.g4mesoft.core.GSIModuleProvider;
import com.g4mesoft.core.client.GSControllerClient;
import com.g4mesoft.core.server.GSControllerServer;
import com.g4mesoft.planner.module.GSPlannerModule;

import net.fabricmc.api.ModInitializer;

public class G4mespeedPlannerMod implements ModInitializer, GSIModuleProvider {

	private static final String MOD_NAME = "G4mespeed Planner";
	
	public static final Logger GSP_LOGGER = LogManager.getLogger(MOD_NAME);
	
	@Override
	public void onInitialize() {
		GSP_LOGGER.info(MOD_NAME + " initialized!");
		
		GSControllerClient.getInstance().addModuleProvider(this);
		GSControllerServer.getInstance().addModuleProvider(this);
	}

	@Override
	public void initModules(GSIModuleManager manager) {
		// This has to create a brand new module, since 
		// it is called on both the client and server!!
		manager.addModule(new GSPlannerModule());
	}
}
