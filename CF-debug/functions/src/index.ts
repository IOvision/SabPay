import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import * as nodemailer from 'nodemailer';
import * as CircularJSON from 'circular-json'
import { totp } from 'otplib'

const default_api_key = 'qIEvxBbP8V6e1YLXICde';
admin.initializeApp()

export const generateOTP = 
functions.region('asia-east2').https.onRequest((req, res) => {
    res.contentType('json');
    const a = Date.now()
    totp.options = {
        epoch: a, 
        step: 300,
        digits: 6
    }
    const secret = <string> req.query.number;
    const token = totp.generate(secret)
    admin.firestore().collection(`user`).where('phone', '==', `+91${secret}`).get()
    .then(user => {
        if(user.docs.length == 0) {
            res.statusCode = 404
            return res.send({
                status: res.statusCode,
            })
        }
        const push_token = user.docs[0].data()?.instanceId
        if(push_token == null){
            res.send("No token/instanceID found")
        }
        const payload = {
            token : push_token,
            notification : {
                title: "OTP for Biometric Enrollment",
                body: `Your OTP for biometric enrollment ${token} is valid for the next 5 minutes.`
            }
        }
        return admin.messaging().send(payload)
        .then(() => {
            res.statusCode = 200;
            return res.send({
                statusCode: res.statusCode,
                otp: token,
                secretnumber: a
            })
        }).catch((error) => {return res.send(error)})
    }).catch((error) => {return res.send(error)})
})
export const verifyOTP = 
functions.region('asia-east2').https.onRequest((req, res) => {
    const secret = <string> req.query.number;
    const otp = <string> req.query.otp;
    const time = parseInt(<string>req.query.time)
    totp.options = {
        epoch: time, 
        step: 300,
        digits: 6
    }
    const isValid = totp.check(otp, secret);
    res.statusCode = 200;
    res.send({
        statusCode: res.statusCode,
        bool: isValid
    });
})
export const notify = 
functions.region('asia-east2').https.onRequest((req, res)=>{
    //url/notify?to={1234567890}?title={title}&msg={msg}&merch=0/1
    // https://us-central1-sabpay-ab94e.cloudfunctions.net/notify
    let to = ""+req.query.to
    const title= ""+req.query.title
    let merch = ""+req.query.merch
    let message = ""+req.query.msg
    res.statusCode = 200;

    const template = (msg)=>{
        return {
            status: res.statusCode,
            msg: msg
        }
    }

    let isMob = true;
    if(to.length<=0){
        res.statusCode = 401;
        res.send(template("Wrong mobile number"));
        return;
    }else if(to.length==10){
        to = `+91${to}`;
    }else{
        isMob = false;
    }
    let pr;
    if(isMob){
        pr = admin.firestore().collection('user').where('phone', '==', to);
    }else{
        pr = admin.firestore().collection('user').where('uid', '==', to);
    }
    pr.get()
    .then(udSnap => {
        if(udSnap.docs.length==0){
            return res.send(template("User Not Found"))
        }
        const ud = udSnap.docs[0]
        let push_token;
        if(merch==="0" || merch===null){
            push_token = ud.data().instanceId;
        }else{
            push_token = ud.data().merchantInstanceId;
        }
        if(push_token===null){
            return res.send(template("No token/ instanceID found"));
        }
        const payload = {
            token: push_token,
            notification: {
                  title: title,
                  body: message
                }
            }
        return admin.messaging().send(payload)
        .then(()=>{
            return res.send(template('Notified'));
        })
        .catch((error) => {
            res.statusCode = 500;
            return res.send(template('Server Error'))});
    }).catch((error) => {
        res.statusCode = 500;
        return res.send(template('Server Error'))});
})
export const placeOrder = 
functions.region('asia-east2').https.onRequest((req, res)=>{
    res.contentType('json');
    if(req.method != 'POST'){ 
        res.statusCode = 405;
        res.setHeader('Content-Type', 'application/json');
        res.send({status: res.statusCode, error: `${req.method} method Not Supported`});
        return;
    }
    /*
    body: {
        orders: [
            {
                base_amount: 0,
                discount: 0 // in percent,
                total_amount: 0,
                transactionId: "",
                items:[{
                    id: "doc id",
                    inventory_id: "string",
                    title: "",
                    description: "",
                    unit: "L/KG/M..",
                    qty: 0,
                    cost: 0
                }..],
                promo:{
                    code: FIRST50,
                    data: "Description of code",
                    tAndC: "Contraints where this is applicable",
                    type: 100/101 // flat or percentage,
                    value: 0
                }
            },
            {},
            {}
        ]
    }
    */
   /*
   return: {
       orders:[
           {
                orderId: "",
                
           }
       ]
   }
   */

    const userId = req.query.id;
    const api_key = req.query.api_key

    if(api_key!==default_api_key){
        res.statusCode = 402;
        res.send({status: res.statusCode, error: `Invalid Api Key`});
        return;
    }

    if(userId==null || userId.length==0){
        res.statusCode = 400;
        res.send({status: res.statusCode, error: `Missing or invalid query params`});
        return;
    }

    const invoices: any[] = [];
    //const os: any[] = [];
     
    const body = req.body
    const itemParams = ['items', 'inventoryId', 'transactionId','discount'];
    const orders: any[] = body.orders;

    if(body==null){
        res.statusCode = 422;
        res.send({status: res.statusCode, error: `Missing or invalid query params`});
        return;
    }
    if(!body.hasOwnProperty('orders')){
        res.statusCode = 422;
        res.send({status: res.statusCode, error: `Missing field order`});
        return;
    }

    const task: any[] = []

    orders.forEach(o=>{
        itemParams.forEach(key=>{
            if(!o.hasOwnProperty(key)){
                res.statusCode = 422;
                res.send({status: res.statusCode, error: `Missing field ${key}`, order: o});
                return;
            }
        });

        const order = {
            id: admin.firestore().collection('order').doc().id,
            user: userId, 
            status: 'PENDING',
            inv: o.inventoryId,
            totalItems: o.items.length,
            amount: 0, // final amount after discount
            items: <any>null, // [{items}] if transaction is not done else null
            invoice: <any>null,
            timestamp: admin.firestore.FieldValue.serverTimestamp()
        }

        if(o.transactionId===null){
            order.items = o.items;
            task.push(admin.firestore().doc(`order/${order.id}`).set(order));
            //os.push(order);
            return;
        }

        const invoice = {
            id: admin.firestore().collection(`user/${userId}/invoice`).doc().id,
            orderId: admin.firestore().doc(`order/${order.id}`),
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            items: o.items,
            discount: <number>o.discount,
            transaction: admin.firestore().doc(`user/${userId}/transaction/${o.transactionId}`),
            base_amount: 0, // without discount
            total_amount: 0 // after discount
        };

        (<any[]>o.items).forEach(item => {
            invoice.base_amount += <number>item.cost*<number>item.qty;
            task.push(
               admin.firestore().doc(`item/${item.id}`)
               .update('qty', admin.firestore.FieldValue.increment(-<number>item.qty))
            );
        });
        invoice.total_amount = invoice.base_amount - invoice.base_amount*invoice.discount/100;

        order.status = 'COMPLETED';
        order.amount = invoice.total_amount;
        order.invoice = admin.firestore().doc(`user/${userId}/invoice/${invoice.id}`);

        invoices.push(invoice);
        //os.push(order)

        task.push(admin.firestore().doc(`user/${userId}/invoice/${invoice.id}`).set(invoice));
        task.push(admin.firestore().doc(`order/${order.id}`).set(order));

    });

    const obj = {
        invoice: invoices,
        status: res.statusCode,
        
    };
    //res.send(responseToSend);

    Promise.all(task)
    .then(() => {
        res.statusCode = 200;
        const json = CircularJSON.stringify(obj);
        res.send(json);
    }).catch(err => {
        console.log(err)
        res.statusCode = 522;
        res.send({
            status: res.statusCode, 
            error: <string>err
        });
    })
})
export const test = 
functions.region('asia-east2').https.onRequest((req, res)=>{
    const arr = ""+req.query.m;
    const animals = arr.split(',');
    
    res.send(animals);
})
export const generateInvoice =
functions.region('asia-east2').https.onRequest((req, res)=>{
    res.contentType('json');
    if(req.method != 'POST'){ 
        res.statusCode = 405;
        res.send({status: res.statusCode, error: `${req.method} method Not Supported`});
        return;
    }
    const api_key = req.query.api_key

    if(api_key!==default_api_key){
        res.statusCode = 402;
        res.send({status: res.statusCode, error: `Invalid query params: Api Key`});
        return;
    }

    const body = req.body
    const itemParams = ['orderId', 'transactionId','discount'];
    if(body===null){
        res.statusCode = 422;
        res.send({status: res.statusCode, error: `Empty Body`});
        return;
    }
    itemParams.forEach(key=>{
        if(!body.hasOwnProperty(key)){
            res.statusCode = 422;
            res.send({status: res.statusCode, error: `Missing field ${key} in body`, body: body});
            return;
        }
    });

    const orderId = body.orderId;
    const discount = body.discount;
    const transactionId = body.transactionId;

    admin.firestore().doc(`order/${orderId}`).get()
    .then(orderRef=>{
        if(!orderRef.exists){
            res.statusCode = 200;
            return res.send({status: res.statusCode, error: `No order with id: ${orderId}`});
        }

        const order = orderRef.data();

        if(order?.items==null){
            res.statusCode = 200
            return res.send({
                status: res.statusCode, 
                message: 'Invoice Already Generated',
                invoiceRef: order?.invoice.id
            });
        }

        const task: any[] = [];
        const invoices: any[]=[];

        const invoice = {
            id: admin.firestore().collection(`user/${order?.user}/invoice`).doc().id,
            orderId: admin.firestore().doc(`order/${order?.id}`),
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            items: order?.items,
            discount: <number>discount,
            transaction: admin.firestore().doc(`user/${order?.user}/transaction/${transactionId}`),
            base_amount: 0, // without discount
            total_amount: 0 // after discount
        };

        order?.items.forEach(item => {
            invoice.base_amount += <number>item.cost*<number>item.qty;
            task.push(
               admin.firestore().doc(`item/${item.id}`)
               .update('qty', admin.firestore.FieldValue.increment(-<number>item.qty))
            );
        });
        invoice.total_amount = invoice.base_amount - invoice.base_amount*invoice.discount/100;

        invoices.push(invoice);

        task.push(
            orderRef.ref.update({
                items: null,
                status: 'COMPLETE',
                amount: invoice.total_amount,
                invoice: admin.firestore().doc(`user/${order?.userId}/invoice/${invoice.id}`)
            })
        );

        const obj = {
            invoice: invoices,
            status: res.statusCode,
            size: invoices.length
        };

        return Promise.all(task)
        .then(()=>{
            res.statusCode = 200;
            const json = CircularJSON.stringify(obj);
            return res.send(json);
        })
        .catch(err => {
            console.log(err)
            res.statusCode = 522;
            res.send({
            status: res.statusCode, 
            error: 'Internal Server Error'
            });
        })
        
    })
    .catch(err => {
        console.log(err)
        res.statusCode = 522;
        res.send({
            status: res.statusCode, 
            error: 'Internal Server Error'
        });
    })
})
export const biometric_IO = 
functions.region('asia-east2').https.onRequest((req, res)=>{
    /*
    {
        template: "",
        id: "",
    }
    id => 10 digit mobile number
    template => finger template in string format
    */
    res.contentType('json');
    const api_key = req.query.api_key

    if(api_key!==default_api_key){
        res.statusCode = 402;
        res.send({status: res.statusCode, error: `Invalid Api Key`});
        return;
    }
    if(req.method == "GET"){
        // url => base_url/biometric_IO?mobile=123456780&api_key=jhklsd
        const mobile = <string>req.query.mobile;

        if(mobile==null || mobile.length!=10){
            res.statusCode = 402;
            res.send({status: res.statusCode, error: `Invalid mobile number`});
            return;
        }

        admin.firestore().doc(`biometric/${mobile}`).get()
        .then((bio)=>{
            if(!bio.exists){
                res.statusCode = 204;
                return res.send({status: res.statusCode, 
                data: `Mobile Number ${mobile} has no biometric data`});
            }
            const biometric = bio.data()?.template;
            const uid = bio.data()?.uid;
            res.statusCode = 200;
            return res.send({status: res.statusCode, 
                template: biometric,
                uid: uid});
        })
        .catch(err => {
            console.log(err)
            res.statusCode = 522;
            res.send({
            status: res.statusCode, 
            error: 'Internal Server Error'});
        })

    }
    else if(req.method == 'POST'){

        const body = req.body;
        const fields = ['template', 'mobile'];

        if(body==null){
            res.statusCode = 422;
            res.send({
                status: res.statusCode, 
                error: `Empty body`});
            return;
        }
        fields.forEach(element => {
            if(!body.hasOwnProperty(element)){
                res.statusCode = 422;
                res.send({status: res.statusCode, 
                    error: `Body doesn has ${element} property`});
                return;
            }
        });
        const mobile = <string>body.mobile;
        const template = <string>body.template;
        if(mobile==null || mobile.length!=10){
            res.statusCode = 422;
            res.send({status: res.statusCode, 
                error: `Invalid mobile number: ${mobile}`});
            return;
        }
        if(template==null){
            res.statusCode = 422;
            res.send({status: res.statusCode, 
                error: `Invalid template`});
            return;
        }
        admin.firestore().collection(`user`).where('phone', '==', `+91${mobile}`).get()
        .then(userData => {
            if(userData.docs.length==0){
                res.statusCode = 422;
                res.send({status: res.statusCode,
                    error: `User does not exist +91${mobile}`});
                return;
            }
            const uid = <string> userData.docs[0].data().uid
            return admin.firestore().doc(`biometric/${mobile}`).create({
                template: template,
                uid: uid 
            })
            .then(()=>{
                res.statusCode = 200;
                res.send({status: res.statusCode, 
                    data: `Document successfully created at path: biometric/${mobile}`});
            })
            .catch(err => {
                console.log(err)
                res.statusCode = 522;
                res.send({
                status: res.statusCode, 
                error: 'Internal Server Error'});
            })
        }).catch(err => {
            console.log(err)
            res.statusCode = 522;
            res.send({
            status: res.statusCode, 
            error: 'Internal Server Error'});
        })
    }
    else{
        res.statusCode = 405;
        res.send({status: res.statusCode, error: `${req.method} not supported`})
    }
})

export const refund =
functions.region('asia-east2').https.onRequest((req,res)=>{
    res.contentType('json');
    if(req.method != 'GET'){ 
        res.statusCode = 405;
        res.send({status: res.statusCode, error: `${req.method} method Not Supported`});
        return;
    }
    const userId = req.query.userId;
    const txnId = req.query.transactionId;
    const api_key = req.query.api_key

    if(api_key!==default_api_key){
        res.statusCode = 402;
        res.send({status: res.statusCode, error: `Invalid query params: Api Key`});
        return;
    }
    if(userId==null||userId.length==0){
        res.statusCode = 402;
        res.send({status: res.statusCode, error: `Invalid query params: UserId`});
        return;
    }
    if(txnId==null||txnId.length==0){
        res.statusCode = 402;
        res.send({status: res.statusCode, error: `Invalid query params: TransactionId`});
        return;
    }
    admin.firestore().doc(`user/${userId}/transaction/${txnId}`).get()
    .then(txnData=>{
        if(!txnData.exists){
            res.statusCode = 200;
            return res.send({status: res.statusCode, error: `User with id: ${userId} has no transaction with id: ${txnId}`});
        }
        const transaction = txnData.data();
        const task: any[] = [];

        const fromRef = <admin.firestore.DocumentReference>transaction?.from;
        const toRef = <admin.firestore.DocumentReference>transaction?.to;
        const amount = <number>transaction?.amount;

        const updateTime = admin.firestore.FieldValue.serverTimestamp();

        task.push(
            fromRef.collection('wallet').doc('wallet').update('balance', admin.firestore.FieldValue.increment(amount))
        );
        task.push(
            toRef.collection('wallet').doc('wallet').update('balance', admin.firestore.FieldValue.increment(-amount))
        );
        task.push(
            admin.firestore().doc(`user/${userId}/transaction/${txnId}`).update({
                'type': -1,
                'timestamp': updateTime
            })
        );
        task.push(
            toRef.collection('transaction').doc(`${txnId}`).update({
                'type': -1,
                'timestamp': updateTime
            })
        );

        return Promise.all(task)
        .then(()=>{
            res.statusCode = 200;
            return res.send({status: res.statusCode, error: `Amount Refund Successfully`, amount: amount});
        })
        .catch(err => {
            console.log(err)
            res.statusCode = 522;
            res.send({
            status: res.statusCode, 
            error: 'Internal Server Error'});
        })
    })
    .catch(err => {
        console.log(err)
        res.statusCode = 522;
        res.send({
        status: res.statusCode, 
        error: 'Internal Server Error'
        });
    })
})

export const newComplain = 
functions.region('asia-east2').firestore.document('complains/{cId}')
.onCreate((dt) => {
    const complain = dt.data()

    const transporter = nodemailer.createTransport({
        service: 'gmail',
        auth: {
            user: 'beta.visionio@gmail.com',
            pass: 'fhrvvrlioyrmkpvw'
        }
    });

    return admin.firestore().doc(`user/${complain?.from}`).get()
    .then(userDt => {
        const user = userDt.data()

        const header = 'Thanks for registering complain with us.'
        const complaidId = `Your complain reference number is ${complain.id}`
        const detail = 'Our expert will look into it and reach back to you in 30 min.'
        const body = `${header}\n\n${complaidId}\n${detail}`


        const mailOptions = {
            from: 'Beta Vision <beta.visionio@gmail.com>', // Something like: Jane Doe <janedoe@gmail.com>
            to: user?.email,
            subject: `no-reply Complain id ${complain.id} Successfuly Registered`, // email subject
            html: `<p style="font-size: 16px;">${body}</p>
                <br />
            ` // email content in HTML
        };

       
        transporter.sendMail(mailOptions, (erro: any, info: any) => {
            if(erro){
                console.log(erro)
            }
            console.log("Mail sended")
        });

        return admin.firestore().doc('complains/meta-data')
        .update('idIndex', admin.firestore.FieldValue.increment(1))
        .catch(error => { console.log(error)} );
    })

})
export const transaction_api = 
functions.region('asia-east2').https.onRequest((req, res)=>{
    res.contentType('json');
    const from = <string> req.query.from;// senders uid
    var to: any =  req.query.to;// receivers mobile number
    var isMob = true; // determines whether to has mob or id
    var amount: any = req.query.amount;// amount to send
    const api_key = req.query.api_key;// api key from admin

    if(from==undefined||to==undefined||amount==undefined||api_key==undefined){
        res.statusCode = 400;
        res.send({status: res.statusCode, error: "undefined values"});
        return;
    }
    if(api_key!==default_api_key){
        res.statusCode = 401;
        res.send({status: res.statusCode, error: "Bad API Key"});
        return;
    }
    if(to.length<=0){
        res.statusCode = 401;
        res.send({status: res.statusCode, error: "Wrong mobile number"});
        return;
    }else if(to.length==10){
        to = `+91${to}`;
    }else{
        isMob = false;
    }
    if(parseFloat(amount.toString())==0.0){
        res.statusCode = 401;
        res.send({status: res.statusCode, error: "Payment amount must be greater than 0"});
        return;
    }else{
        amount = parseFloat(amount.toString());
    }

    const from_user = admin.firestore().collection('user').doc(from);

    return from_user.get().then(from_dt => {
        let pr;
        if(isMob){
            pr = admin.firestore().collection('user').where('phone', '==', to);
        }else{
            pr = admin.firestore().collection('user').where('uid', '==', to);
        }

        return pr.get()
        .then(to_dt=>{
            if(to_dt.docs.length==0){
                res.statusCode = 200;
                res.send({status: res.statusCode, error: "User not found"});
            }
            return from_user.collection('wallet').doc('wallet').get()
            .then(wallet_dt=>{
                if(<number>wallet_dt.data()?.balance < amount){
                    res.statusCode = 200;
                    res.send({status: res.statusCode, error: "Insufficient Balance"});
                    return;
                }
                const to_user = to_dt.docs[0];
                const txnId = to_user.ref.collection('transaction').doc().id;
                const txnObj = {
                    amount: amount,
                    id: txnId,
                    from: from_user,
                    to: to_user.ref,
                    timestamp: admin.firestore.FieldValue.serverTimestamp(),
                    type: 0
                }
                const transaction_movment: any = []

                transaction_movment.push(
                    from_user.collection('transaction').doc(txnId).set(txnObj)
                );
                transaction_movment.push(
                    to_user.ref.collection('transaction').doc(txnId).set(txnObj)
                );
                transaction_movment.push(
                    from_user.collection('wallet').doc('wallet').update(
                        {
                            balance: admin.firestore.FieldValue.increment(-amount),
                            lastTransaction: from_user.collection('transaction').doc(txnId)
                        }
                    )
                );
                transaction_movment.push(
                    to_user.ref.collection('wallet').doc('wallet').update(
                        {
                            balance: admin.firestore.FieldValue.increment(+amount),
                            lastTransaction: to_user.ref.collection('transaction').doc(txnId)
                        }
                    )
                );
                return Promise.all(transaction_movment)
                .then(()=>{
                    res.statusCode = 200;
                    return res.send({status: res.statusCode, 
                        transactionId: txnId,
                        from: from_dt.data()?.name, 
                        amount: amount});
                })
                .catch(error=>{
                    res.statusCode = 500;
                    console.error(error);
                    return res.send({status: res.statusCode, error: error});
                })
            })
            .catch(error=>{
                res.statusCode = 500;
                console.error(error);
                return res.send({status: res.statusCode, error: error});
            })
        })
        .catch(error=>{
            res.statusCode = 500;
            console.error(error);
            return res.send({status: res.statusCode, error: error});
        })
    }).catch(error => {
        res.statusCode=500;
        return res.send({status: res.statusCode, error: error});
    })
})
export const newTransaction = 
functions.region('asia-east2').firestore.document('user/{userId}/pending_transaction/transaction')
.onWrite((change) => {
    const transactionObject = change.after.data()

    const fromDocumentRef = (<admin.firestore.DocumentReference> transactionObject?.from)
    const toDocumentRef: Array<admin.firestore.DocumentReference> = transactionObject?.to

    const transaction_promises: any[] = []

    const toMap = new Map()
    let lastFromTrxnId: string = "";
    const suphix = ['A','B','C','D','E','F','G','H','I','J',
    'K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z']
    let i=0
    toDocumentRef.forEach(payee => {
        const transaction_template = {
            id: String(transactionObject?.id).replace(/.$/, suphix[i]),
            amount: transactionObject?.amount,
            from: transactionObject?.from,
            to: payee,
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            type: transactionObject?.type
        }
        transaction_promises.push(
            payee.collection('transaction')
            .doc(transaction_template.id).set(transaction_template)
        )
        transaction_promises.push(
            fromDocumentRef.collection('transaction')
            .doc(transaction_template.id).set(transaction_template)
        )
        toMap.set(payee, transaction_template.id)
        i++
        if(i==toDocumentRef.length){
            lastFromTrxnId = transaction_template.id
        }
    })

    return Promise.all(transaction_promises).then((result) => {
        const update_recent_trnsaction_promises: any = []

        const fromTransactionRef = fromDocumentRef.collection('transaction').doc(lastFromTrxnId)

        const add_recent_ransaction_in_from = fromDocumentRef.collection('wallet').doc('wallet')
        .update('lastTransaction', fromTransactionRef)
        update_recent_trnsaction_promises.push(add_recent_ransaction_in_from)

        for(let entry of toMap){
            const ref = <admin.firestore.DocumentReference> entry[0]?.collection('transaction')
            .doc(entry[1])
            update_recent_trnsaction_promises.push(
                entry[0].collection('wallet').doc('wallet')
                .update('lastTransaction', ref)
            )
        }

        return Promise.all(update_recent_trnsaction_promises).then(output => {
            const wallet_amount_update_promises: any = []

            const amount = parseInt(transactionObject?.amount)
        
            const update_wallet_amount_in_from = fromDocumentRef.collection('wallet').doc('wallet')
            .update('balance', admin.firestore.FieldValue.increment(-(amount*toDocumentRef.length)))
            wallet_amount_update_promises.push(update_wallet_amount_in_from)

            for(let entry of toMap){
                update_recent_trnsaction_promises.push(
                    entry[0].collection('wallet').doc('wallet')
                    .update('balance', admin.firestore.FieldValue.increment(amount))
                )
            }
        
            return Promise.all(wallet_amount_update_promises).catch(error => {
                console.log(error)
            })
        
        }).catch(error => {
            console.log(error)
        })
    }).catch(error => {
        console.log(error)
    })   
})
export const gPayTransaction = 
functions.region('asia-east2').firestore
.document('user/{userId}/pending_gPay_transactions/{tId}')
.onUpdate((transactionData, context) => {

    const transaction = transactionData.after.data()

    const toRef = <admin.firestore.DocumentReference> transaction?.to

    const fromRef = <admin.firestore.DocumentReference> transaction?.from

    const groupRef = toRef.collection('group_pay').doc('meta-data')
    .collection('transaction').doc(transaction?.gPayId);

    return groupRef.get().then((change) => {
        const data = change.data()

        const ledger: Array<number> = data?.ledger

        let sum: number = 0;

        ledger.forEach((element: number) => {
            sum = sum + element
        });

        let activeStatus: boolean = data?.active
        let updateAmount = parseInt(transaction?.amount)

        if(sum+updateAmount >= parseInt(data?.amount)){
            updateAmount = data?.amount-sum
            activeStatus = false
        }

        ledger.push(updateAmount)
    
        const updatedData = {
            active: activeStatus,
            amount: data?.amount,
            ledger: ledger,
            timestamp: data?.timestamp
        }

        const fromDocumentRef = (<admin.firestore.DocumentReference> transaction?.from)

        const promises: any = []
    
        const updatedTransaction = {
            id: transaction?.id,
            from: transaction?.from,
            to: transaction?.to,
            gPayId: transaction?.gPayId,
            type: 1,
            timestamp: transaction?.timestamp,
            amount: updateAmount
        }

        promises.push(
            groupRef.collection('transaction').doc(transaction?.id).set(updatedTransaction))

        promises.push(
            fromRef.collection('transaction').doc(updatedTransaction.id).set(updatedTransaction)
        )

        promises.push(
            fromRef.collection('wallet').doc('wallet')
            .update('balance', admin.firestore.FieldValue.increment(-updateAmount))
        )

        promises.push(
            toRef.collection('wallet').doc('wallet')
            .update('balance', admin.firestore.FieldValue.increment(updateAmount))
        )

        return Promise.all(promises).then(() => {
            return fromDocumentRef.collection('wallet').doc('wallet')
            .update('lastTransaction', fromRef.collection('transaction').doc(transaction?.id))
            .then(() => {
                return groupRef.update(updatedData)
            }).then(()=>{
                return fromRef.collection('pending_gPay_transactions')
                .doc(context.params.tId).delete().catch(error => {
                    console.log(error)
                })
            }).catch(error => {
                console.log(error)
            })
        }).catch(error => {
            console.log(error)
        })
    }).catch(error => {
        console.log(error)
    });
    
})
export const splitGpay = 
functions.region('asia-east2').https.onRequest((req, res)=>{
    // base/splitGpay?to={}&gPayId={}&m={m1,m2,m3....mn}&gId={}
    const mUids = (""+req.query.m).split('_');
    const toId = ""+req.query.to;
    const gPayId = ""+req.query.gPayId;
    const groupId = ""+req.query.gId;

    
    const to = admin.firestore().doc(`user/${toId}`)

    const temp = (msg)=>{
        return {
            status: res.statusCode,
            msg: msg
        }
    }

    if(mUids===undefined||to===undefined||gPayId===undefined||groupId===undefined){
        res.statusCode = 400;
        res.send(temp(`Invalid Query Parameters`));
    }

    let members: Array<admin.firestore.DocumentReference> = [];

    for(const mUid of mUids){
        members.push(
            admin.firestore().doc(`user/${mUid}`)
        )
    }

    const splitPromises: any = [];

    for (const member of members) {
        const id = member.collection('pending_gPay_transactions').doc().id
        const data = {
            id: id,
            amount: null,
            from: member,
            to: to,
            gPayId: gPayId,
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            type: 1
        }

        splitPromises.push(member.collection('pending_gPay_transactions').doc(id).set(data))
    }


    res.statusCode = 200;
    Promise.all(splitPromises).then(() => {
        return to.collection('group_pay/meta-data/transaction').doc(gPayId).update({
            from: admin.firestore().doc(`groups/${groupId}`),
            parts: members.length
        }).then(()=>{
            res.send(temp("splitted"));
        }).catch(()=>{
            res.statusCode = 500;
            res.send(temp("server Error"));
        })
    }).catch(error => {
        res.statusCode = 500;
        res.send(temp("server Error"));
    })
    
})
export const splitTransaction = 
functions.region('asia-east2').firestore
.document('/groups/{grouId}/transactions/{id}')
.onCreate((result, context) => {
    const transaction = result.data()

    const groupId = context.params.grouId

    return admin.firestore().doc(`groups/${groupId}`).get().then(groupResult => {
        const group = groupResult.data()

        const splitPromises: any = []

        const members: Array<admin.firestore.DocumentReference> = group?.members
        
        for (const member of members) {
            const id = member.collection('pending_gPay_transactions').doc().id
            const data = {
                id: id,
                amount: null,
                from: member,
                to: transaction?.to,
                gPayId: transaction?.id,
                timestamp: admin.firestore.FieldValue.serverTimestamp(),
                type: 1
            }

            splitPromises.push(member.collection('pending_gPay_transactions').doc(id).set(data))
        }


        return Promise.all(splitPromises).then(() => {
            return (<admin.firestore.DocumentReference> transaction?.to)
            .collection('group_pay/meta-data/transaction').doc(transaction?.id).update({
                from: admin.firestore().doc(`groups/${groupId}`),
                parts: members.length
            })
        }).catch(error => {
            console.log(error)
        })

    })

})