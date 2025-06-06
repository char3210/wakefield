/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.security.ec;

import java.io.*;
import java.security.interfaces.XECPrivateKey;
import java.util.Optional;
import java.security.*;
import java.security.spec.*;

import sun.security.pkcs.PKCS8Key;
import sun.security.x509.AlgorithmId;
import sun.security.util.*;

public final class XDHPrivateKeyImpl extends PKCS8Key implements XECPrivateKey {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("serial") // Type of field is not Serializable
    private final NamedParameterSpec paramSpec;
    private byte[] k;

    XDHPrivateKeyImpl(XECParameters params, byte[] k)
            throws InvalidKeyException {

        this.paramSpec = new NamedParameterSpec(params.getName());
        this.k = k.clone();

        this.algid = new AlgorithmId(params.getOid());

        DerValue val = new DerValue(DerValue.tag_OctetString, k);
        try {
            this.privKeyMaterial = val.toByteArray();
        } finally {
            val.clear();
        }
        checkLength(params);
    }

    XDHPrivateKeyImpl(byte[] encoded) throws InvalidKeyException {
        super(encoded);
        XECParameters params = XECParameters.get(
            InvalidKeyException::new, algid);
        paramSpec = new NamedParameterSpec(params.getName());
        try {
            DerInputStream derStream = new DerInputStream(privKeyMaterial);
            k = derStream.getOctetString();
        } catch (IOException ex) {
            throw new InvalidKeyException(ex);
        }
        checkLength(params);
    }

    void checkLength(XECParameters params) throws InvalidKeyException {

        if (params.getBytes() != this.k.length) {
            throw new InvalidKeyException(
                "key length must be " + params.getBytes());
        }
    }

    public byte[] getK() {
        return k.clone();
    }

    @Override
    public String getAlgorithm() {
        return "XDH";
    }

    @Override
    public AlgorithmParameterSpec getParams() {
        return paramSpec;
    }

    @Override
    public Optional<byte[]> getScalar() {
        return Optional.of(getK());
    }

    public PublicKey calculatePublicKey() {
        XECParameters params = paramSpec.getName().equalsIgnoreCase("X25519")
                ? XECParameters.X25519
                : XECParameters.X448;
        try {
            return new XDHPublicKeyImpl(params,
                    new XECOperations(params).computePublic(k.clone()));
        } catch (InvalidKeyException e) {
            throw new ProviderException(
                    "Unexpected error calculating public key", e);
        }
    }

    /**
     * Restores the state of this object from the stream.
     * <p>
     * Deserialization of this object is not supported.
     *
     * @param  stream the {@code ObjectInputStream} from which data is read
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a serialized class cannot be loaded
     */
    @java.io.Serial
    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        throw new InvalidObjectException(
                "XDHPrivateKeyImpl keys are not directly deserializable");
    }
}
