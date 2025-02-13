package com.github.applejuiceyy.figuraextras.mixin.figura.networking;

import com.github.applejuiceyy.figuraextras.ipc.IPCManager;
import com.github.applejuiceyy.figuraextras.views.avatar.http.NetworkView;
import net.minecraft.util.Tuple;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.figuramc.figura.backend2.HttpAPI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Mixin(value = HttpAPI.class, remap = false)
public abstract class HttpAPIMixin {
    @Inject(method = "runString", at = @At("HEAD"), cancellable = true)
    static private void overrideString(HttpRequest request, BiConsumer<Integer, String> consumer, CallbackInfo ci) {
        if (IPCManager.INSTANCE.divertBackend.shouldDivertBackend()) {
            diversion(request, consumer);
            ci.cancel();
        }
    }

    @Inject(method = "runStream", at = @At("HEAD"), cancellable = true)
    static private void overrideStream(HttpRequest request, BiConsumer<Integer, InputStream> consumer, CallbackInfo ci) {
        if (IPCManager.INSTANCE.divertBackend.shouldDivertBackend()) {
            diversion(request, (i, s) -> {
                try {
                    consumer.accept(i, new ByteArrayInputStream(Hex.decodeHex(s)));
                } catch (DecoderException e) {
                    throw new RuntimeException(e);
                }
            });
            ci.cancel();
        }
    }

    @Unique
    static private void diversion(HttpRequest request, BiConsumer<Integer, String> consumer) {
        if (!IPCManager.INSTANCE.divertBackend.isBackendConnected()) {
            return;
        }
        String name = Path.of(request.uri().getPath()).getFileName().toString();

        String method = request.method();
        if (method.equals("POST")) {
            method = "set";
        }
        name = method.toLowerCase() + Character.toUpperCase(name.charAt(0)) + name.substring(1);

        String rawQuery = request.uri().getRawQuery();

        Map<String, String> query = rawQuery == null ? new HashMap<>() : Arrays.stream(rawQuery.split("&")).map(str -> {
                    String[] split = str.split("=");
                    return new Tuple<>(split[0], URLDecoder.decode(split[1], StandardCharsets.UTF_8));
                })
                .collect(Collectors.toMap(Tuple::getA, Tuple::getB));

        request.bodyPublisher().map(p -> {
            var bodySubscriber = HttpResponse.BodySubscribers.ofByteArray();
            var flowSubscriber = new NetworkView.StringSubscriber(bodySubscriber);
            p.subscribe(flowSubscriber);
            return bodySubscriber.getBody().toCompletableFuture().join();
        }).ifPresent(s -> query.put("body", Hex.encodeHexString(s)));

        CompletableFuture<?> requested = IPCManager.INSTANCE.getC2CServer().getBackend().request("backend/" + name, query.isEmpty() ? null : query);

        try {
            consumer.accept(200, (String) requested.get(5, TimeUnit.SECONDS));
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ResponseErrorException err) {
                String message = err.getResponseError().getMessage();
                if (message.equals("not logged in")) {
                    consumer.accept(401, null);
                    return;
                }
                if (err.getResponseError().getCode() == ResponseErrorCode.MethodNotFound.getValue()) {
                    consumer.accept(404, null);
                    return;
                }
                consumer.accept(500, null);
                return;
            }
            throw new RuntimeException(e.getCause());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            consumer.accept(500, null);
        }
    }

    @Shadow
    protected abstract HttpRequest.Builder header(String url);

    @Inject(method = "getUser", at = @At("HEAD"), cancellable = true)
    private void overrideGetUser(UUID id, CallbackInfoReturnable<HttpRequest> cir) {
        if (IPCManager.INSTANCE.divertBackend.shouldDivertBackend()) {
            cir.setReturnValue(header("user?id=" + id.toString()).build());
        }
    }

    @Inject(method = "getAvatar", at = @At("HEAD"), cancellable = true)
    private void overrideGetAvatar(UUID owner, String id, CallbackInfoReturnable<HttpRequest> cir) {
        if (IPCManager.INSTANCE.divertBackend.shouldDivertBackend()) {
            cir.setReturnValue(header("avatar?owner=" + owner.toString() + "&id=" + URLEncoder.encode(id, Charset.defaultCharset())).build());
        }
    }

    @Inject(method = "uploadAvatar", at = @At("HEAD"), cancellable = true)
    private void overrideUploadAvatar(String id, byte[] bytes, CallbackInfoReturnable<HttpRequest> cir) {
        if (IPCManager.INSTANCE.divertBackend.shouldDivertBackend()) {
            cir.setReturnValue(header("avatar?id=" + id).PUT(HttpRequest.BodyPublishers.ofByteArray(bytes)).build());
        }
    }

    @Inject(method = "deleteAvatar", at = @At("HEAD"), cancellable = true)
    private void overrideDeleteAvatar(String id, CallbackInfoReturnable<HttpRequest> cir) {
        if (IPCManager.INSTANCE.divertBackend.shouldDivertBackend()) {
            cir.setReturnValue(header("avatar?id=" + id).DELETE().build());
        }
    }
}
