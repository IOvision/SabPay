import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
admin.initializeApp()

export const newTransaction = 
functions.firestore.document('user/{userId}/pending_transaction/transaction')
.onWrite((change) => {
    const transactionObject = change.after.data()

    const fromDocumentRef = (<admin.firestore.DocumentReference> transactionObject?.from)
    const toDocumentRef = (<admin.firestore.DocumentReference> transactionObject?.to)

    //const increment = admin.firestore.FieldValue.increment(<number> transactionObject?.amount)
    //const decrement = admin.firestore.FieldValue.increment(- (<number> transactionObject?.amount))

    const transaction_promises = []

    const add_transaction_in_from = fromDocumentRef
    .collection('transaction').doc(String(transactionObject?.id)).set(<Object> transactionObject);
    transaction_promises.push(add_transaction_in_from)

    const add_transaction_in_to = toDocumentRef
    .collection('transaction').doc(String(transactionObject?.id)).set(<Object> transactionObject)
    transaction_promises.push(add_transaction_in_to)

    return Promise.all(transaction_promises).then((result) => {
        const update_recent_trnsaction_promises = []

        const fromTransactionRef = fromDocumentRef.collection('transaction').doc(transactionObject?.id)
        const toTransactionRef = toDocumentRef.collection('transaction').doc(transactionObject?.id)

        const add_recent_ransaction_in_from = fromDocumentRef.collection('wallet').doc('wallet')
        .update('lastTransaction', fromTransactionRef,)
        update_recent_trnsaction_promises.push(add_recent_ransaction_in_from)

        const add_recent_ransaction_in_to = toDocumentRef.collection('wallet').doc('wallet')
        .update('lastTransaction', toTransactionRef)
        update_recent_trnsaction_promises.push(add_recent_ransaction_in_to)

        return Promise.all(update_recent_trnsaction_promises).then(output => {
            const wallet_amount_update_promises = []

            const amount = parseInt(transactionObject?.amount)
            console.log(-amount)
        
            const update_wallet_amount_in_from = fromDocumentRef.collection('wallet').doc('wallet')
            .update('balance', admin.firestore.FieldValue.increment(-amount))
            wallet_amount_update_promises.push(update_wallet_amount_in_from)
            
            const update_wallet_amount_in_to = toDocumentRef.collection('wallet').doc('wallet')
            .update('balance', admin.firestore.FieldValue.increment(amount))
            wallet_amount_update_promises.push(update_wallet_amount_in_to)
        
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
            parts: data?.parts + 1,
            timestamp: data?.timestamp
        }

        const fromDocumentRef = (<admin.firestore.DocumentReference> transaction?.from)

        const promises = []
    
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

        const splitPromises = []

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
                from: admin.firestore().doc(`groups/${groupId}`)
            })
        }).catch(error => {
            console.log(error)
        })

    })

})


