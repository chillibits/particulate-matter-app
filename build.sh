#!/bin/bash
# Secret encryption for Travis
tar cvf secrets.tar app/src/main/res/values/keystore.xml
travis encrypt-file secrets.tar secrets.tar.enc --add --org