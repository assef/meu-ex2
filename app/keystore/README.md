# keystore — AOSP platform test-key (PÚBLICA)

Estes arquivos são a **chave de plataforma de teste pública do AOSP**
(`build/target/product/security/platform.{pk8,x509.pem}` do próprio Android):

```
subject = C=US, ST=California, L=Mountain View, O=Android, OU=Android,
          CN=Android, emailAddress=android@android.com
```

Não é segredo — é a mesma chave que acompanha o código-fonte do Android há anos.
Ela está aqui porque **o firmware desta central (IHU629G) confia nela**: um APK
assinado com esta chave e com `sharedUserId=android.uid.system` é aceito como app
de sistema. É exatamente o que permite o Meu EX2 acessar as APIs do carro.

## Implicação de segurança

Que o ROM confie na test-key pública é uma fraqueza **do firmware**, não deste
projeto — qualquer um pode assinar um app de sistema pra essa central. O Meu EX2
só aproveita isso; não introduz a fraqueza.

Se um dia você tiver a sua própria platform key (ROM re-assinado), troque estes
dois arquivos pelos seus.
