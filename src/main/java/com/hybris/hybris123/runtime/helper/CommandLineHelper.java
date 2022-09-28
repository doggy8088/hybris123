package com.hybris.hybris123.runtime.helper;
/*
 * © 2017 SAP SE or an SAP affiliate company.
 * All rights reserved.
 * Please see http://www.sap.com/corporate-en/legal/copyright/index.epx for additional trademark information and
 * notices.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.annotation.ManagedBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ManagedBean
public class CommandLineHelper {
	
	private static final Logger LOG = LoggerFactory.getLogger(CommandLineHelper.class);
	
	private CommandLineHelper() {
	}

    public static String runCmd(String cmd) {
        String output = "";
    	  try {
    		  if (!cmd.equals("git --git-dir ../hybris/.git log") &&
    				  !cmd.equals("mvn.cmd --version") &&
    				  !cmd.equals("mvn --version") &&
    				  !cmd.equals("git --version") )
    			  throw new IllegalArgumentException("Unexpected command.");

    		  Process p = Runtime.getRuntime().exec(cmd);
              BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
              String s = "";
              int maxlines = 1000;
              while ((s = br.readLine()) != null && maxlines-- > 0) {
            	  output = output.concat(s);
              }
              p.waitFor();
              p.destroy();            	
          } catch (IOException | InterruptedException | IllegalArgumentException e) {
        	  LOG.error(e.getLocalizedMessage());
        	  return "EXCEPTION: " + e;
          }
    	  LOG.info(output);
    	  return output;
    }
}

