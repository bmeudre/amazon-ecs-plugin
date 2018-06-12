/*
 * The MIT License
 *
 *  Copyright (c) 2015, CloudBees, Inc.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 */

package com.cloudbees.jenkins.plugins.amazonecs;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsHelper;
import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;
import hudson.AbortException;
import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulates interactions with Amazon EC2.
 *
 * @author Jan Roehrich <jan@roehrich.info>
 *
 */
class EC2Service {
    private static final Logger LOGGER = Logger.getLogger(ECSCloud.class.getName());

    private String credentialsId;

    private String regionName;

    public EC2Service(String credentialsId, String regionName) {
        super();
        this.credentialsId = credentialsId;
        this.regionName = regionName;
    }

    AmazonEC2Client getAmazonEC2Client() {
        final AmazonEC2Client client;

        ProxyConfiguration proxy = Jenkins.getInstance().proxy;
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        if(proxy != null) {
            clientConfiguration.setProxyHost(proxy.name);
            clientConfiguration.setProxyPort(proxy.port);
            clientConfiguration.setProxyUsername(proxy.getUserName());
            clientConfiguration.setProxyPassword(proxy.getPassword());
        }

        AmazonWebServicesCredentials credentials = getCredentials(credentialsId);
        if (credentials == null) {
            // no credentials provided, rely on com.amazonaws.auth.DefaultAWSCredentialsProviderChain
            // to use IAM Role define at the EC2 instance level ...
            client = new AmazonEC2Client(clientConfiguration);
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                String awsAccessKeyId = credentials.getCredentials().getAWSAccessKeyId();
                String obfuscatedAccessKeyId = StringUtils.left(awsAccessKeyId, 4) + StringUtils.repeat("*", awsAccessKeyId.length() - (2 * 4)) + StringUtils.right(awsAccessKeyId, 4);
                LOGGER.log(Level.FINE, "Connect to Amazon EC2 with IAM Access Key {1}", new Object[]{obfuscatedAccessKeyId});
            }
            client = new AmazonEC2Client(credentials, clientConfiguration);
        }
        client.setRegion(getRegion(regionName));
        LOGGER.log(Level.FINE, "Selected Region: {0}", regionName);
        return client;
    }

    Region getRegion(String regionName) {
        if (StringUtils.isNotEmpty(regionName)) {
            return RegionUtils.getRegion(regionName);
        } else {
            return Region.getRegion(Regions.US_EAST_1);
        }
    }

    @CheckForNull
    private AmazonWebServicesCredentials getCredentials(@Nullable String credentialsId) {
        return AWSCredentialsHelper.getCredentials(credentialsId, Jenkins.getActiveInstance());
    }

}
