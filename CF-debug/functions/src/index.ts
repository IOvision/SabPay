import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import * as PaytmChecksum from '../lib/PaytmChecksum';

admin.initializeApp()

export const generateChecksum = 
functions.region('asia-east2').https.onRequest((req, res) => {
    return admin.firestore().collection("public").doc("Paytm").get()
        .then(data => {
            const mid = data.data()?.MID
            const website = data.data()?.WEBSITE
            const id = admin.firestore().collection("addMoney").doc().id;
            const callback = data.data()?.CALLBACK_URL + id
            const txnAmount = req.query.amount
            const userId = req.query.uid

            var paytmParams: any = {};
            
            paytmParams.body = {
                "requestType"   : "Payment",
                "mid"           : mid,
                "websiteName"   : website,
                "orderId"       : id,
                "callbackUrl"   : callback,
                "txnAmount"     : {
                    "value"     : txnAmount,
                    "currency"  : "INR",
                },
                "userInfo"      : {
                    "custId"    : userId,
                },
            };

            var checksum = PaytmChecksum.generateSignature(JSON.stringify(paytmParams.body), key);
            checksum.then((result) => {
                paytmParams.head = {
                    "signature" : result
                }
                admin.firestore().doc(`/addMoney/${id}`).set({
                    custId : userId
                }).then(() => {
                    res.contentType('application/json');
                    return res.send(JSON.stringify(paytmParams))
                }).catch((error) => {return res.send(error)});
            }).catch((error) => {return res.send(error)});
        });
})
export const paytmCallback = 
functions.region('asia-east2').https.onRequest((req, res) => {
    
    const orderId = req.body.ORDERID
    const respcode = (req.body.RESPCODE as unknown) as number
    const amount = req.body.TXNAMOUNT
    
    if(respcode == 0o1){
        admin.firestore().collection('addMoney').doc(orderId).get()
        .then(data => {
            const uid = data.data()?.custId;
            console.log(uid)
            admin.firestore().collection('user').doc(uid).collection('wallet')
            .doc('wallet').update({
                balance : admin.firestore.FieldValue.increment(parseFloat(amount))
            }).then(() => {
                res.send({
                    uid: uid,
                })
            }).catch((error) => {res.send({error: error.message})});
        }).catch((error) => {res.send({error: error.message})});
    }
})