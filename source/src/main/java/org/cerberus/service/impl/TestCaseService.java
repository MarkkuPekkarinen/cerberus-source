/*
 * Cerberus  Copyright (C) 2013  vertigo17
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This file is part of Cerberus.
 *
 * Cerberus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cerberus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cerberus.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cerberus.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.cerberus.dao.ITestCaseDAO;
import org.cerberus.entity.TCase;
import org.cerberus.entity.TestCase;
import org.cerberus.entity.TestCaseCountry;
import org.cerberus.entity.TestCaseCountryProperties;
import org.cerberus.entity.TestCaseStep;
import org.cerberus.entity.TestCaseStepAction;
import org.cerberus.entity.TestCaseStepActionControl;
import org.cerberus.exception.CerberusException;
import org.cerberus.factory.IFactoryTCase;
import org.cerberus.service.ITestCaseCountryPropertiesService;
import org.cerberus.service.ITestCaseCountryService;
import org.cerberus.service.ITestCaseService;
import org.cerberus.service.ITestCaseStepActionControlService;
import org.cerberus.service.ITestCaseStepActionService;
import org.cerberus.service.ITestCaseStepService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author bcivel
 * @author tbernardes
 */
@Service
public class TestCaseService implements ITestCaseService {

    @Autowired
    private ITestCaseDAO testCaseDao;
    @Autowired
    private ITestCaseCountryService testCaseCountryService;
    @Autowired
    private ITestCaseCountryPropertiesService testCaseCountryPropertiesService;
    @Autowired
    private ITestCaseStepService testCaseStepService;
    @Autowired
    private ITestCaseStepActionService testCaseStepActionService;
    @Autowired
    private ITestCaseStepActionControlService testCaseStepActionControlService;
    @Autowired
    private IFactoryTCase factoryTCase;

    @Override
    public TCase findTestCaseByKey(String test, String testCase) throws CerberusException {
        return testCaseDao.findTestCaseByKey(test, testCase);
    }

    @Override
    public TCase findTestCaseByKeyWithDependency(String test, String testCase) throws CerberusException {
        TCase newTcase;
        newTcase = findTestCaseByKey(test, testCase);
        List<TestCaseCountry> testCaseCountry = testCaseCountryService.findTestCaseCountryByTestTestCase(test, testCase);
        List<TestCaseCountry> testCaseCountryToAdd = new ArrayList();
        for (TestCaseCountry tcc : testCaseCountry) {
            List<TestCaseCountryProperties> properties = testCaseCountryPropertiesService.findListOfPropertyPerTestTestCaseCountry(test, testCase, tcc.getCountry());
            tcc.setTestCaseCountryProperty(properties);
            testCaseCountryToAdd.add(tcc);
        }
        newTcase.setTestCaseCountry(testCaseCountryToAdd);

        String initialTest = test;
        String initialTc = testCase;
        List<TestCaseStep> tcs = testCaseStepService.getListOfSteps(test, testCase);
        List<TestCaseStep> tcsToAdd = new ArrayList();
        for (TestCaseStep step : tcs) {
            int stepNumber = step.getStep();
            int initialStep = step.getStep();
            if (step.getUseStep().equals("Y")) {
                test = step.getUseStepTest();
                testCase = step.getUseStepTestCase();
                stepNumber = step.getUseStepStep();
            }
            List<TestCaseStepAction> tcsa = testCaseStepActionService.getListOfAction(test, testCase, stepNumber);
            List<TestCaseStepAction> tcsaToAdd = new ArrayList();
            for (TestCaseStepAction action : tcsa) {
                List<TestCaseStepActionControl> tcsac = testCaseStepActionControlService.findControlByTestTestCaseStepSequence(test, testCase, stepNumber, action.getSequence());
                List<TestCaseStepActionControl> tcsacToAdd = new ArrayList();
                for (TestCaseStepActionControl control : tcsac) {
                    control.setTest(initialTest);
                    control.setTestCase(initialTc);
                    control.setStep(initialStep);
                    tcsacToAdd.add(control);
                }
                action.setTestCaseStepActionControl(tcsacToAdd);
                action.setTest(initialTest);
                action.setTestCase(initialTc);
                action.setStep(initialStep);
                tcsaToAdd.add(action);
            }
            step.setTestCaseStepAction(tcsaToAdd);
            tcsToAdd.add(step);
        }
        newTcase.setTestCaseStep(tcsToAdd);
        return newTcase;
    }

    @Override
    public List<TCase> findTestCaseByTest(String test) {
        return testCaseDao.findTestCaseByTest(test);
    }

    @Override
    public boolean updateTestCaseInformation(TestCase testCase) {
        return testCaseDao.updateTestCaseInformation(testCase);
    }

    @Override
    public boolean updateTestCaseInformationCountries(TestCase tc) {
        return testCaseDao.updateTestCaseInformationCountries(tc);
    }

    @Override
    public boolean createTestCase(TCase testCase) throws CerberusException {
        return testCaseDao.createTestCase(testCase);
    }

    @Override
    public List<TCase> findTestCaseActiveByCriteria(String test, String application, String country) {
        return testCaseDao.findTestCaseByCriteria(test, application, country, "Y");
    }

    /**
     * @since 0.9.1
     */
    @Override
    public List<TCase> findTestCaseByAllCriteria(TCase tCase, String text, String system) {
        return this.testCaseDao.findTestCaseByCriteria(tCase, text, system);
    }

    /**
     * @param column
     * @return
     * @since 0.9.1
     */
    @Override
    public List<String> findUniqueDataOfColumn(String column) {
        return this.testCaseDao.findUniqueDataOfColumn(column);
    }

    @Override
    public List<String> findTestWithTestCaseActiveAutomatedBySystem(String system) {
        TCase tCase = factoryTCase.create(null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, -1, null, null, null, null, null, "Y",
                null, null, null, null, null, null, null, null, null, null, null, null, null);

        List<String> result = new ArrayList();
        List<TCase> testCases = findTestCaseByAllCriteria(tCase, null, system);
        for (TCase testCase : testCases) {
            if (!testCase.getGroup().equals("PRIVATE")) {
                result.add(testCase.getTest());
            }
        }
        Set<String> uniqueResult = new HashSet<String>(result);
        result = new ArrayList();
        result.addAll(uniqueResult);
        Collections.sort(result);
        return result;
    }

    @Override
    public List<TCase> findTestCaseActiveAutomatedBySystem(String test, String system) {
        TCase tCase = factoryTCase.create(test, null, null, null, null, null, null, null, null, null,
                null, null, null, null, -1, null, null, null, null, null, "Y",
                null, null, null, null, null, null, null, null, null, null, null, null, null);

        List<TCase> result = new ArrayList();
        List<TCase> testCases = findTestCaseByAllCriteria(tCase, null, system);
        for (TCase testCase : testCases) {
            if (!testCase.getGroup().equals("PRIVATE")) {
                result.add(testCase);
            }
        }
        return result;
    }

    @Override
    public boolean deleteTestCase(TCase testCase) {
        return testCaseDao.deleteTestCase(testCase);
    }

    @Override
    public void updateTestCaseField(TCase tc, String columnName, String value) {
        testCaseDao.updateTestCaseField(tc, columnName, value);
    }

    /**
     * @since 1.0.2
     */
    @Override
    public List<TCase> findTestCaseByGroupInCriteria(TCase tCase, String system) {
        return this.testCaseDao.findTestCaseByGroupInCriteria(tCase, system);
    }

    @Override
    public void updateTestCase(TCase tc) throws CerberusException {
        testCaseDao.updateTestCase(tc);
    }

    @Override
    public List<TCase> findTestCaseByCampaignName(String campaign) {
        return testCaseDao.findTestCaseByCampaignName(campaign);
    }

}
