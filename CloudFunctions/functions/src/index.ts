import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
admin.initializeApp()

export const newTransaction = 
functions.firestore.document('user/{userId}/pending_transaction/transaction')
.onUpdate((change) => {
    const transactionObject = change.after.data()

    const fromDocumentRef = (<admin.firestore.DocumentReference> transactionObject?.from)
    const toDocumentRef = (<admin.firestore.DocumentReference> transactionObject?.to)

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

        const add_recent_ransaction_in_from = fromDocumentRef
        .collection('wallet').doc('wallet').update('lastTransaction', fromTransactionRef)
        update_recent_trnsaction_promises.push(add_recent_ransaction_in_from)

        const add_recent_ransaction_in_to = toDocumentRef
        .collection('wallet').doc('wallet').update('lastTransaction', toTransactionRef)
        update_recent_trnsaction_promises.push(add_recent_ransaction_in_to)

        return Promise.all(update_recent_trnsaction_promises).catch(error => {
            console.log(error)
        })
    }).catch(error => {
        console.log(error)
    })
    
})

/* export const onNewTransaction = 
functions.firestore.document('user/{userId}/pending_transaction/transaction')
.onUpdate(async (change) => {

    try {

        const transactionObject = change.after.data()

        const fromDocumentRef = (<admin.firestore.DocumentReference> transactionObject?.from)
        const toDocumentRef = (<admin.firestore.DocumentReference> transactionObject?.to)

        const transaction_promises = []

        const add_transaction_in_from = fromDocumentRef
        .collection('transaction').doc(String(transactionObject?.id)).set(<Object> transactionObject);
        transaction_promises.push(add_transaction_in_from)

        const add_transaction_in_to = toDocumentRef
        .collection('transaction').doc(String(transactionObject?.id)).set(<Object> transactionObject)
        transaction_promises.push(add_transaction_in_to)

        await Promise.all(transaction_promises)

        const update_recent_trnsaction_promises = []

        const fromTransactionRef = fromDocumentRef.collection('transaction').doc(transactionObject?.id)
        const toTransactionRef = toDocumentRef.collection('transaction').doc(transactionObject?.id)

        const add_recent_ransaction_in_from = fromDocumentRef
        .collection('wallet').doc('wallet').update('lastTransaction', fromTransactionRef)
        update_recent_trnsaction_promises.push(add_recent_ransaction_in_from)

        const add_recent_ransaction_in_to = toDocumentRef
        .collection('wallet').doc('wallet').update('lastTransaction', toTransactionRef)
        update_recent_trnsaction_promises.push(add_recent_ransaction_in_to)

        
    } catch (error) {
        console.log(error)
    }
}) */


