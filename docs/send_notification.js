var FCM = require('fcm-push');
var firebaseConfig = {
  projectId: 'bitwalking-33f77',
  webApiKey: 'AIzaSyBIGYW6DBYrdgFJR5hJYDmWisnr1Byqp_I',
  serverKey: 'AIzaSyDop4dyQ0oJpYTNPavsgQ91VbM7q6h1FaQ'
};

var serverKey = firebaseConfig.serverKey;
var fcm = new FCM(serverKey);

var toUser = '';

var message = {
    // to topic, currently available: news (all users), bitwalking (users with email @bitwalking.com), debug (debug versions)
    // to: "/topics/news",

    // to specific user, replace with relevant push_token
    to: 'dxjLAKIicVA:APA91bFBHTrsGR1SEmE3jJriTjPspgPpKLX5frG_FVEXdQlg6mWwgElxC7nYuBzeF7_iFTgT59huTfR4XGMgE55EL3l9SmB_Cxq1jPrM8cjGfyVVfPq9vdYkAmkd7tp2isGe06UT0_1U', // required fill with device token or topics
    data: {
        // extra_open: 'Bitwalking.Store'           // Open store activity
        // extra_open: 'Balance'                    // Show balance slide
        // extra_open: 'PlayStore'                  // Open Play Store
        // extra_open: 'Bitwalking.UserInvite'      // Open user invite activity
        // extra_open: 'Bitwalking.Events',         // Open events activity
        // extra_data: '5'                          // Open specific event, event_id = 5
        extra_open: 'SendLogs'                      // Send logs to server
        // extra_open: 'Update!'                    // Force update
        // open_uri: "http://bitwalking.com"        // Open uri, in app
    },

    // notification: {
    //     title: 'Take steps towards change',
    //     body: 'Users in Latin America, join the Nokia-Bitwalking event',
    //     icon: 'notification_icon', // this will make the app use right icon
    //     click_action: 'OPEN_EXTRA'
    // }

    notification: {
        title: 'Bitwalking support',
        body: 'Please click to help us analyze Bitwalking app problem.',
        icon: 'notification_icon',      // this will make the app use right icon
        click_action: 'OPEN_EXTRA'      // Used to match MainActivity intent filter
    }
};

//promise style
fcm.send(message)
    .then(function(response){
        console.log("Successfully sent with response: ", response);
    })
    .catch(function(err){
        console.log("Something has gone wrong!");
        console.error(err);
    })
