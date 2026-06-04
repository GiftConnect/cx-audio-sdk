import Foundation
import UIKit

internal final class VastAdLoader {
    static let shared = VastAdLoader()

    private let session: URLSession
    private let parser = VastParser()
    private let maxWrapperDepth = 5

    init(session: URLSession = .shared) {
        self.session = session
    }

    func loadAudioMediaFile(
        request: VastRequest,
        completion: @escaping (Result<VastMediaFile, Error>) -> Void
    ) {
        resolve(
            url: request.vastTagURL,
            request: request,
            depth: 0,
            select: { document, baseURL in
                document.bestAudioMediaFile(baseURL: baseURL)
            },
            missingError: CxAudioError.noAudioMediaFile,
            completion: completion
        )
    }

    func loadCompanion(
        request: VastRequest,
        completion: @escaping (Result<VastCompanion, Error>) -> Void
    ) {
        resolve(
            url: request.vastTagURL,
            request: request,
            depth: 0,
            select: { document, baseURL in
                document.bestCompanion(baseURL: baseURL)
            },
            missingError: CxAudioError.noImageCompanion,
            completion: completion
        )
    }

    func loadStickyPlayerAd(
        request: VastRequest,
        completion: @escaping (Result<VastStickyPlayerAd, Error>) -> Void
    ) {
        resolve(
            url: request.vastTagURL,
            request: request,
            depth: 0,
            select: { document, baseURL in
                guard let mediaFile = document.bestAudioMediaFile(baseURL: baseURL),
                      let companion = document.bestCompanion(baseURL: baseURL) else {
                    return nil
                }
                return VastStickyPlayerAd(mediaFile: mediaFile, companion: companion)
            },
            missingError: CxAudioError.invalidOption(
                "VAST response did not include both audio media and an image companion"
            ),
            completion: completion
        )
    }

    func loadInterstitial(
        request: VastRequest,
        completion: @escaping (Result<VastInterstitialAd, Error>) -> Void
    ) {
        resolve(
            url: request.vastTagURL,
            request: request,
            depth: 0,
            select: { document, baseURL in
                document.interstitialAd(baseURL: baseURL)
            },
            missingError: CxAudioError.noUsableInterstitial,
            completion: completion
        )
    }

    func loadImage(
        url: URL,
        request: VastRequest,
        completion: @escaping (Result<UIImage, Error>) -> Void
    ) {
        let urlRequest = makeURLRequest(url: url, request: request)
        session.dataTask(with: urlRequest) { data, response, error in
            if let error {
                completion(.failure(error))
                return
            }
            if let httpResponse = response as? HTTPURLResponse,
               !(200..<300).contains(httpResponse.statusCode) {
                completion(.failure(CxAudioError.httpStatus(httpResponse.statusCode, url)))
                return
            }
            guard let data, let image = UIImage(data: data) else {
                completion(.failure(CxAudioError.imageDecodeFailed(url)))
                return
            }
            completion(.success(image))
        }.resume()
    }

    private func resolve<T>(
        url: URL,
        request: VastRequest,
        depth: Int,
        select: @escaping (VastDocument, URL) -> T?,
        missingError: Error,
        completion: @escaping (Result<T, Error>) -> Void
    ) {
        guard depth <= maxWrapperDepth else {
            completion(.failure(CxAudioError.wrapperDepthExceeded(maxWrapperDepth)))
            return
        }

        loadDocument(url: url, request: request) { [weak self] result in
            guard let self else {
                completion(.failure(CxAudioError.playbackFailed("VAST loader was released")))
                return
            }

            switch result {
            case .success(let document):
                if let selected = select(document, url) {
                    completion(.success(selected))
                    return
                }

                if let wrapperURLString = document.wrapperURLString,
                   let wrapperURL = URL(string: wrapperURLString, relativeTo: url)?.absoluteURL {
                    self.resolve(
                        url: wrapperURL,
                        request: request,
                        depth: depth + 1,
                        select: select,
                        missingError: missingError,
                        completion: completion
                    )
                } else {
                    completion(.failure(missingError))
                }
            case .failure(let error):
                completion(.failure(error))
            }
        }
    }

    private func loadDocument(
        url: URL,
        request: VastRequest,
        completion: @escaping (Result<VastDocument, Error>) -> Void
    ) {
        let urlRequest = makeURLRequest(url: url, request: request)
        session.dataTask(with: urlRequest) { [parser] data, response, error in
            if let error {
                completion(.failure(error))
                return
            }
            if let httpResponse = response as? HTTPURLResponse,
               !(200..<300).contains(httpResponse.statusCode) {
                completion(.failure(CxAudioError.httpStatus(httpResponse.statusCode, url)))
                return
            }
            guard let data else {
                completion(.failure(CxAudioError.invalidOption("VAST response was empty")))
                return
            }

            do {
                completion(.success(try parser.parse(data: data)))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }

    private func makeURLRequest(url: URL, request: VastRequest) -> URLRequest {
        var urlRequest = URLRequest(url: url, timeoutInterval: request.timeoutInterval)
        urlRequest.setValue(request.userAgent, forHTTPHeaderField: "User-Agent")
        return urlRequest
    }
}
