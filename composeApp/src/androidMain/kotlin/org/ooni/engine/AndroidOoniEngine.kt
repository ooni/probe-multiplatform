package org.ooni.engine

class AndroidOoniEngine : OoniEngine {
    override fun newUUID4(): String {
        return oonimkall.Oonimkall.newUUID4()
    }
}