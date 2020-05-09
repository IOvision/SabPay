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

export const newGpayTransaction = 
functions.firestore
.document('/user/{userId}/group_pay/meta-data/transaction/{transactiosId}/transactions/{id}')
.onCreate((transactionData, context) => {

    const transaction = transactionData.data()

    return admin.firestore()
    .doc(`/user/${context.params.userId}/group_pay/meta-data/transaction/${context.params.transactiosId}`).get().then((change) => {
        const data = change.data()

        const ledger: Array<number> = data?.ledger

        let sum: number = 0;

        ledger.forEach((element: number) => {
            sum = sum + element
        });

        let activeStatus: boolean = data?.active
        let updateAmount = parseInt(transaction?.amount)
        let isAmountUpdated = false;

        if(sum+updateAmount >= parseInt(data?.amount)){
            updateAmount = data?.amount-sum
            activeStatus = false
            isAmountUpdated = true
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
    

        if(isAmountUpdated){
            promises.push(
                admin.firestore()
                .doc(`/user/${context.params.userId}/group_pay/meta-data/transaction/${context.params.transactiosId}/transactions/${context.params.id}`)
                .update('amount', updateAmount)
            )
        }

        promises.push(
            fromDocumentRef.collection('transaction').doc(transaction?.id).set({
                from: transaction?.from,
                to: transaction?.to,
                type: 1,
                timestamp: transaction?.timestamp,
                amount: updateAmount
            }))

        promises.push(
            fromDocumentRef.collection('wallet').doc('wallet')
            .update('balance', admin.firestore.FieldValue.increment(-updateAmount))
        )

        promises.push(
            admin.firestore().doc(`/user/${context.params.userId}/wallet/wallet`)
            .update('balance', admin.firestore.FieldValue.increment(updateAmount))
        )

        return Promise.all(promises).then(() => {
            return fromDocumentRef.collection('wallet').doc('wallet')
            .update('lastTransaction', fromDocumentRef.collection('transaction').doc(transaction?.id))
            .then(() => {
                return admin.firestore()
                .doc(`/user/${context.params.userId}/group_pay/meta-data/transaction/${context.params.transactiosId}`)
                .update(updatedData)
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


