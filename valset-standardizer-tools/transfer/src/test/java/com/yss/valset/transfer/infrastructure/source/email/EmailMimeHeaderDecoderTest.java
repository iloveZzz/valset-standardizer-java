package com.yss.valset.transfer.infrastructure.source.email;

import org.junit.jupiter.api.Test;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmailMimeHeaderDecoderTest {

    @Test
    void decodesUtf8Base64WordsSplitInsideMultibyteCharacter() {
        String expected = "\u8d22\u901a\u57fa\u91d1\u5149\u8363\u591a\u7b56\u7565-2026-04-13";
        byte[] bytes = expected.getBytes(StandardCharsets.UTF_8);
        String header = "=?utf-8?B?"
                + Base64.getEncoder().encodeToString(Arrays.copyOfRange(bytes, 0, 1))
                + "?= =?utf-8?B?"
                + Base64.getEncoder().encodeToString(Arrays.copyOfRange(bytes, 1, bytes.length))
                + "?=";

        assertEquals(expected, EmailMimeHeaderDecoder.decodeHeader(header));
    }

    @Test
    void wadasd() throws Exception{

        String wwws1 = "=?UTF-8?B?U1RGODQwX+WbveazsOWQm+WuiQ==?=\n" +
                "=?UTF-8?B?5pyf6LSn5YWJ5aSn55CG6LSi6YeP5YyWMeWPt1P?=\n" +
                "=?UTF-8?B?TembhuWQiOi1hOS6p+euoeeQhuiuoeWIkl/otYtkuqc=?=\n" +
                "=?UTF-8?B?5Lyw5YC86KGoXzIwMjYwNTEy?=";
        MimeMessage messagss1 = new MimeMessage(Session.getInstance(new Properties()));
        messagss1.setHeader("Subject", wwws1);
        System.out.println(EmailMimeHeaderDecoder.decodeSubject(messagss1));
    }

    @Test
    void decodesRawSubjectHeaderBeforeJavaMailSubjectFallback() throws Exception {
        String expected = "\u4f30\u503c\u8868\u6279\u91cf\u91cd\u65b0\u89e3\u6790";
        byte[] bytes = expected.getBytes(StandardCharsets.UTF_8);
        String header = "=?utf-8?B?"
                + Base64.getEncoder().encodeToString(Arrays.copyOfRange(bytes, 0, 4))
                + "?=\r\n =?utf-8?B?"
                + Base64.getEncoder().encodeToString(Arrays.copyOfRange(bytes, 4, bytes.length))
                + "?=";
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        message.setHeader("Subject", header);

        assertEquals(expected, EmailMimeHeaderDecoder.decodeSubject(message));
    }

    @Test
    void usesStandardMailApiWhenSubjectIsHealthy() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        message.setSubject("\u6b63\u5e38\u90ae\u4ef6\u4e3b\u9898", StandardCharsets.UTF_8.name());

        assertEquals("\u6b63\u5e38\u90ae\u4ef6\u4e3b\u9898", EmailMimeHeaderDecoder.decodeSubject(message));
    }

    @Test
    void keepsPlainSubjectUnchanged() {
        assertEquals("plain subject", EmailMimeHeaderDecoder.decodeHeader("plain subject"));
    }

    @Test
    void decodesNormalAdjacentBase64WordsWithPadding() {
        String header = "=?utf-8?B?5q2j5bi4?= =?utf-8?B?6YKu5Lu25Li76aKY?=";

        assertEquals("\u6b63\u5e38\u90ae\u4ef6\u4e3b\u9898", EmailMimeHeaderDecoder.decodeHeader(header));
    }

    @Test
    void preservesPlainTextBetweenEncodedWords() {
        String header = "RE: =?utf-8?B?5q2j5bi4?= - =?utf-8?B?6YKu5Lu25Li76aKY?=";

        assertEquals("RE: \u6b63\u5e38 - \u90ae\u4ef6\u4e3b\u9898", EmailMimeHeaderDecoder.decodeHeader(header));
    }

    @Test
    void decodesQEncodedAdjacentWords() {
        String header = "=?utf-8?Q?=E6=AD=A3=E5=B8=B8?= =?utf-8?Q?=E9=82=AE=E4=BB=B6?=";

        assertEquals("\u6b63\u5e38\u90ae\u4ef6", EmailMimeHeaderDecoder.decodeHeader(header));
    }
}
