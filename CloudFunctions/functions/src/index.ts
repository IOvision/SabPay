import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import * as nodemailer from 'nodemailer';
admin.initializeApp()

export const newComplain = 
functions.firestore.document('complains/{cId}')
.onCreate((dt, context) => {
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
        const detail = 'Our expert will look into it and reach out within 2 business days.'
        const contact = 'Meanwhile you can ring us at +91 1234567890 for any query'
        const body = `${header}\n\n${complaidId}\n${detail}\n${contact}`


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

export const newTransaction = 
functions.firestore.document('user/{userId}/pending_transaction/transaction')
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
functions.firestore
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

export const splitTransaction = functions.firestore
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


