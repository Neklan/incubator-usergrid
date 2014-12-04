/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 package org.apache.usergrid.scenarios

import io.gatling.core.Predef._
 import io.gatling.http.Predef._
 import io.gatling.http.request.StringBody
 import org.apache.usergrid.datagenerators.FeederGenerator
 import org.apache.usergrid.settings.{Headers, Settings, Utils}

 object UserScenarios {

  val getRandomUser = exec(
    http("GET user")
      .get("/users/user" + Utils.generateRandomInt(1, Settings.numEntities))
      .headers(Headers.jsonAuthorized)
      .check(status.is(200))
  )

   val getUserByUsername = exec(
     http("GET user")
       .get("/users/${username}")
       .headers(Headers.jsonAuthorized)
       .check(status.saveAs("userStatus"), jsonPath("$..entities[0]").exists, jsonPath("$..entities[0].uuid").exists, jsonPath("$..entities[0].uuid").saveAs("userId"))
   )


   /**
    * Post a user
    */
   val postUser =

     exec(
            http("POST geolocated Users")
              .post("/users")
              .body(new StringBody("""{"location":{"latitude":"${latitude}","longitude":"${longitude}"},"username":"${username}",
           "displayName":"${displayName}","age":"${age}","seen":"${seen}","weight":"${weight}",
           "height":"${height}","aboutMe":"${aboutMe}","profileId":"${profileId}","headline":"${headline}",
           "showAge":"${showAge}","relationshipStatus":"${relationshipStatus}","ethnicity":"${ethnicity}","password":"password"}"""))
              .check(status.saveAs("userStatus"))
              .check(status.is(200),jsonPath("$..entities[0].uuid").saveAs("userId"))
          )


   /**
     * Try to get a user, if it returns a 404, create the user
     */
   val postUserIfNotExists =
     exec(getUserByUsername)
       .doIf ("${userStatus}", "404") {
      exec(postUser)
     }


   val putUser =
     exec(getUserByUsername)
     .doIf("${userStatus}", "200") {
       exec(
         http("POST geolocated Users")
           .put("/users")
           .body(new StringBody( """{"location":{"latitude":"${latitude}","longitude":"${longitude}"},"username":"${username}",
        "displayName":"${displayName}","age":"${age}","seen":"${seen}","weight":"${weight}",
        "height":"${height}","aboutMe":"${aboutMe}","profileId":"${profileId}","headline":"${headline}",
        "showAge":"${showAge}","relationshipStatus":"${relationshipStatus}","ethnicity":"${ethnicity}","password":"password"}"""))
           .check(status.is(200), jsonPath("$..entities[0].uuid").saveAs("userId"))

       )
      }

   val deleteUserByUsername = exec(
     http("DELETE user")
       .delete("/users/${username}")
       .headers(Headers.jsonAuthorized)
       .check(status.is(200), jsonPath("$..entities[0].uuid").saveAs("userId"))
   )

   /**
    * Logs in as the admin user.  Checks if a user exists, if not, creates the user
    * Logs in as the user, then creates 2 devices if they do not exist
    */
   val createUsersWithDevicesScenario =  scenario("Create Users")
     .feed(Settings.getInfiniteUserFeeder())
     .exec(TokenScenarios.getManagementToken)
     .exec(UserScenarios.postUserIfNotExists)
     .exec(TokenScenarios.getUserToken)
     .exec(UserScenarios.getUserByUsername)
     .repeat(2){
       feed(FeederGenerator.generateEntityNameFeeder("device", Settings.numDevices))
         .exec( DeviceScenarios.maybeCreateDevices)
     }

   /**
    * Posts a new user every time
    */
   val postUsersInfinitely =  scenario("Post Users")
        .feed(Settings.getInfiniteUserFeeder())
        .exec(UserScenarios.postUser)
 }
